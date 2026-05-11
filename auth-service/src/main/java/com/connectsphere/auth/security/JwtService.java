package com.connectsphere.auth.security;

import com.connectsphere.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Handles JWT security responsibilities.
 */
@Service
public class JwtService {

    private final String secret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

/**
 * Generates a signed JWT for the authenticated user.
 * @param user method input parameter
 * @return operation result
 */
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey())
                .compact();
    }

/**
 * Extracts the username claim from the supplied JWT.
 * @param token method input parameter
 * @return operation result
 */
    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

/**
 * Verifies that the JWT belongs to the expected user and has not expired.
 * @param token method input parameter
 * @param userDetails method input parameter
 * @return true when the condition is satisfied; otherwise false
 */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && claims(token).getExpiration().after(new Date());
    }

/**
 * Returns expiration seconds.
 * @return operation result
 */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

