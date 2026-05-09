package com.connectsphere.auth.security;

import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Handles Pending OAuth Signup security responsibilities.
 */
@Service
public class PendingOAuthSignupService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "auth:oauth:pending:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PendingOAuthSignupService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

/**
 * Performs the create operation.
 * @param signup method input parameter
 * @return operation result
 */
    public String create(PendingOAuthSignup signup) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(redisKey(token), serialize(signup), TOKEN_TTL);
        return token;
    }

/**
 * Performs the consume operation.
 * @param token method input parameter
 * @return resulting value
 */
    public PendingOAuthSignup consume(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("OAuth setup token is missing");
        }

        String key = redisKey(token);
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            throw new BadRequestException("OAuth setup session expired. Please sign in again.");
        }

        return deserialize(value);
    }

    private String redisKey(String token) {
        return KEY_PREFIX + token;
    }

    private String serialize(PendingOAuthSignup signup) {
        try {
            return objectMapper.writeValueAsString(new StoredPendingOAuthSignup(
                    signup.provider().name(),
                    signup.providerId(),
                    signup.email(),
                    signup.fullName(),
                    signup.profilePicUrl()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to store OAuth setup session", exception);
        }
    }

    private PendingOAuthSignup deserialize(String value) {
        try {
            StoredPendingOAuthSignup signup = objectMapper.readValue(value, StoredPendingOAuthSignup.class);
            return new PendingOAuthSignup(
                    Provider.valueOf(signup.provider()),
                    signup.providerId(),
                    signup.email(),
                    signup.fullName(),
                    signup.profilePicUrl()
            );
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new BadRequestException("OAuth setup session expired. Please sign in again.");
        }
    }

    private record StoredPendingOAuthSignup(
            String provider,
            String providerId,
            String email,
            String fullName,
            String profilePicUrl
    ) {
    }
}
