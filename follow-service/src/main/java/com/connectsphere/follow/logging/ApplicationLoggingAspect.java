package com.connectsphere.follow.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@Aspect
@Component
public class ApplicationLoggingAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Controller *) || within(@org.springframework.stereotype.Service *) || within(@org.springframework.stereotype.Repository *)")
    public void applicationLayer() {
    }

    @Around("applicationLayer()")
    public Object logMethodInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> targetClass = joinPoint.getSignature().getDeclaringType();
        Logger logger = LoggerFactory.getLogger(targetClass);
        String methodName = ((MethodSignature) joinPoint.getSignature()).toShortString();
        Layer layer = resolveLayer(targetClass);
        int argumentCount = joinPoint.getArgs() == null ? 0 : joinPoint.getArgs().length;
        long startTime = System.nanoTime();

        if (logger.isDebugEnabled()) {
            logger.debug("Entering {} with {} argument(s)", methodName, argumentCount);
        }

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            if (layer == Layer.REPOSITORY) {
                logger.debug("Completed {} in {} ms", methodName, elapsedMs);
            } else {
                logger.info("Completed {} in {} ms", methodName, elapsedMs);
            }
            return result;
        } catch (Throwable ex) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.error("Failed {} after {} ms with {}: {}", methodName, elapsedMs, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private Layer resolveLayer(Class<?> targetClass) {
        if (AnnotationUtils.findAnnotation(targetClass, RestController.class) != null
            || AnnotationUtils.findAnnotation(targetClass, Controller.class) != null) {
            return Layer.CONTROLLER;
        }
        if (AnnotationUtils.findAnnotation(targetClass, Service.class) != null) {
            return Layer.SERVICE;
        }
        if (AnnotationUtils.findAnnotation(targetClass, Repository.class) != null) {
            return Layer.REPOSITORY;
        }
        return Layer.OTHER;
    }

    private enum Layer {
        CONTROLLER,
        SERVICE,
        REPOSITORY,
        OTHER
    }
}
