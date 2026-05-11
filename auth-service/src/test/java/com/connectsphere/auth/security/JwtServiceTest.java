package com.connectsphere.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.entity.Role;
import com.connectsphere.auth.entity.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Unit tests for JwtService – covers token generation, extraction, and validation.
 */
class JwtServiceTest {

    /** A 512-bit Base64-encoded secret for HS256 (≥256 bits required by JJWT). */
    private static final String SECRET =
            "dGVzdHNlY3JldGtleWZvcmp3dHVuaXR0ZXN0c2VjcmV0a2V5Zm9yand0dW5pdHRlc3Q=";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600L);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("alice");
        testUser.setEmail("alice@example.com");
        testUser.setRole(Role.USER);
        testUser.setProvider(Provider.LOCAL);
        testUser.setActive(true);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
    }

    // -------------------------------------------------------------------------
    // generateToken
    // -------------------------------------------------------------------------

    @Test
    void generateTokenReturnsNonBlankString() {
        String token = jwtService.generateToken(testUser);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateTokenIsThreePartJwt() {
        String token = jwtService.generateToken(testUser);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must have header.payload.signature");
    }

    // -------------------------------------------------------------------------
    // extractUsername
    // -------------------------------------------------------------------------

    @Test
    void extractUsernameReturnsSubject() {
        String token = jwtService.generateToken(testUser);
        assertEquals("alice", jwtService.extractUsername(token));
    }

    // -------------------------------------------------------------------------
    // isTokenValid
    // -------------------------------------------------------------------------

    @Test
    void isTokenValidReturnsTrueForMatchingUser() {
        String token = jwtService.generateToken(testUser);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("alice");

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValidReturnsFalseForDifferentUsername() {
        String token = jwtService.generateToken(testUser);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("bob");

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValidReturnsFalseForExpiredToken() {
        // Use a negative expiry so the token is expired at the moment of creation.
        // JJWT throws ExpiredJwtException when parsing an expired token's claims.
        JwtService alreadyExpired = new JwtService(SECRET, -1L);
        String token = alreadyExpired.generateToken(testUser);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("alice");

        // JJWT rejects the token immediately with ExpiredJwtException
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> alreadyExpired.isTokenValid(token, userDetails));
    }

    // -------------------------------------------------------------------------
    // getExpirationSeconds
    // -------------------------------------------------------------------------

    @Test
    void getExpirationSecondsReturnsConfiguredValue() {
        assertEquals(3600L, jwtService.getExpirationSeconds());
    }

    // -------------------------------------------------------------------------
    // extractUsername – invalid token throws
    // -------------------------------------------------------------------------

    @Test
    void extractUsernameThrowsForMalformedToken() {
        assertThrows(Exception.class, () -> jwtService.extractUsername("not.a.jwt"));
    }
}
