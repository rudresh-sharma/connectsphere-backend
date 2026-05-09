package com.connectsphere.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class PendingOAuthSignupServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PendingOAuthSignupService service;

    @BeforeEach
    void setUp() {
        service = new PendingOAuthSignupService(redisTemplate, new ObjectMapper());
    }

    @Test
    void createStoresSignupInRedisWithTtl() {
        PendingOAuthSignup signup = new PendingOAuthSignup(
                Provider.GOOGLE,
                "google-123",
                "rudresh@example.com",
                "Rudresh Sharma",
                "https://example.com/profile.jpg"
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = service.create(signup);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofMinutes(10)));
        assertEquals("auth:oauth:pending:" + token, keyCaptor.getValue());
        assertEquals(
                "{\"provider\":\"GOOGLE\",\"providerId\":\"google-123\",\"email\":\"rudresh@example.com\",\"fullName\":\"Rudresh Sharma\",\"profilePicUrl\":\"https://example.com/profile.jpg\"}",
                valueCaptor.getValue()
        );
    }

    @Test
    void consumeReadsAndDeletesSignupFromRedis() {
        String token = "setup-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:oauth:pending:" + token)).thenReturn(
                "{\"provider\":\"GITHUB\",\"providerId\":\"github-123\",\"email\":\"rudresh@example.com\",\"fullName\":\"Rudresh Sharma\",\"profilePicUrl\":null}"
        );

        PendingOAuthSignup signup = service.consume(token);

        assertEquals(Provider.GITHUB, signup.provider());
        assertEquals("github-123", signup.providerId());
        assertEquals("rudresh@example.com", signup.email());
        verify(valueOperations).getAndDelete("auth:oauth:pending:" + token);
    }

    @Test
    void consumeRejectsMissingOrExpiredToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(any())).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.consume("expired-token"));

        assertEquals("OAuth setup session expired. Please sign in again.", exception.getMessage());
    }
}
