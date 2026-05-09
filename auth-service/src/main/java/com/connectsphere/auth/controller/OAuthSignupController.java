package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.AuthResponse;
import com.connectsphere.auth.dto.OAuthCompleteRequest;
import com.connectsphere.auth.dto.UsernameAvailabilityResponse;
import com.connectsphere.auth.entity.Role;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtService;
import com.connectsphere.auth.security.PendingOAuthSignup;
import com.connectsphere.auth.security.PendingOAuthSignupService;
import com.connectsphere.auth.util.UserMapper;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * Exposes OAuth Signup API endpoints.
 */


@RestController
@RequestMapping("/auth")

public class OAuthSignupController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PendingOAuthSignupService pendingOAuthSignupService;

    public OAuthSignupController(
            UserRepository userRepository,
            JwtService jwtService,
            PendingOAuthSignupService pendingOAuthSignupService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.pendingOAuthSignupService = pendingOAuthSignupService;
    }
/**
 * Handles the username available request.
 * @param username method input parameter
 * @return resulting value
 */

    @GetMapping("/username-available")
    public UsernameAvailabilityResponse usernameAvailable(@RequestParam("username") String username) {
        String normalizedUsername = normalizeUsername(username);
        return new UsernameAvailabilityResponse(
                normalizedUsername,
                !userRepository.existsByUsername(normalizedUsername)
        );
    }
/**
 * Completes OAuth signup.
 * @param request request payload
 * @return operation result
 */

    @PostMapping("/oauth/complete")
    public AuthResponse completeOAuthSignup(@Valid @RequestBody OAuthCompleteRequest request) {
        String username = normalizeUsername(request.username());

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is already taken");
        }

        PendingOAuthSignup signup = pendingOAuthSignupService.consume(request.setupToken());

        User user = userRepository.findByEmail(signup.email().toLowerCase(Locale.ROOT))
                .orElseGet(() -> createUser(signup, username));

        if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is already taken");
        }

        user.setUsername(username);
        user.setBio(normalizeBio(request.bio()));
        user.setActive(true);
        User savedUser = userRepository.saveAndFlush(user);

        return new AuthResponse(
                jwtService.generateToken(savedUser),
                "Bearer",
                jwtService.getExpirationSeconds(),
                UserMapper.toResponse(savedUser)
        );
    }

    private User createUser(PendingOAuthSignup signup, String username) {
        return User.builder()
                .username(username)
                .email(signup.email().toLowerCase(Locale.ROOT))
                .fullName(signup.fullName() == null || signup.fullName().isBlank() ? username : signup.fullName())
                .bio(null)
                .passwordHash(null)
                .profilePicUrl(signup.profilePicUrl())
                .role(Role.USER)
                .provider(signup.provider())
                .providerId(signup.providerId())
                .isActive(true)
                .build();
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3 || normalized.length() > 40) {
            throw new BadRequestException("Username must be between 3 and 40 characters");
        }
        if (!normalized.matches("^[a-zA-Z0-9._-]+$")) {
            throw new BadRequestException("Username can only contain letters, numbers, dots, underscores, and hyphens");
        }
        return normalized;
    }

    private String normalizeBio(String bio) {
        if (bio == null) {
            return null;
        }

        String normalized = bio.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
