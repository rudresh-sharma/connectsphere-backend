package com.connectsphere.post.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Unit tests for post-service GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/posts/test");
        when(request.getMethod()).thenReturn("POST");
    }

    @Test
    void handleNotFoundReturns404() {
        ResponseEntity<?> resp = handler.handleNotFound(new ResourceNotFoundException("post not found"), request);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void handleBadRequestReturns400() {
        ResponseEntity<?> resp = handler.handleBadRequest(new BadRequestException("invalid"), request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void handleForbiddenReturns403() {
        ResponseEntity<?> resp = handler.handleForbidden(new ForbiddenException("forbidden"), request);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void handleValidationReturns400WithFieldErrors() {
        FieldError fieldError = new FieldError("obj", "content", "must not be blank");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<?> resp = handler.handleValidation(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void handleValidationWithMultipleErrors() {
        FieldError e1 = new FieldError("obj", "content", "blank");
        FieldError e2 = new FieldError("obj", "authorId", "null");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(e1, e2));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<?> resp = handler.handleValidation(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void handleUnexpectedReturns500() {
        ResponseEntity<?> resp = handler.handleUnexpected(new RuntimeException("crash"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }
}
