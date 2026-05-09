package com.connectsphere.auth.security;

import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Completes the OAuth2 login flow and redirects users either to token-based sign-in or username setup.
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PendingOAuthSignupService pendingOAuthSignupService;
    private final String successUrl;
    private final String failureUrl;
    private final String setupUrl;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            PendingOAuthSignupService pendingOAuthSignupService,
            @Value("${app.frontend.oauth-success-url}") String successUrl,
            @Value("${app.frontend.oauth-failure-url}") String failureUrl,
            @Value("${app.frontend.oauth-setup-url}") String setupUrl
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.pendingOAuthSignupService = pendingOAuthSignupService;
        this.successUrl = successUrl;
        this.failureUrl = failureUrl;
        this.setupUrl = setupUrl;
    }

    /**
     * Handles a successful OAuth2 authentication response from the identity provider.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param authentication successful OAuth2 authentication
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("connectsphereEmail");
            if (email == null || email.isBlank()) {
                email = oauthUser.getAttribute("email");
            }

            if (email == null || email.isBlank()) {
                redirectWithError(response, "OAuth provider did not return an email address");
                return;
            }

            Boolean existingUser = oauthUser.getAttribute("connectsphereExistingUser");
            if (!Boolean.TRUE.equals(existingUser)) {
                // New OAuth users must finish username selection before the platform issues an application JWT.
                redirectToUsernameSetup(response, oauthUser, email);
                return;
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ServletException("OAuth user was not saved"));

            String token = jwtService.generateToken(user);
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            response.sendRedirect(successUrl + "?token=" + encodedToken);
        } catch (Exception ex) {
            redirectWithError(response, ex.getMessage());
        }
    }

    private void redirectWithError(HttpServletResponse response, String message) throws IOException {
        String encodedError = URLEncoder.encode(message == null ? "OAuth login failed" : message, StandardCharsets.UTF_8);
        response.sendRedirect(failureUrl + "?error=" + encodedError);
    }

    private void redirectToUsernameSetup(
            HttpServletResponse response,
            OAuth2User oauthUser,
            String email
    ) throws IOException {
        // Persist the provider context so the frontend can complete signup without re-running the provider login flow.
        String provider = oauthUser.getAttribute("connectsphereProvider");
        String providerId = oauthUser.getAttribute("connectsphereProviderId");
        String fullName = oauthUser.getAttribute("connectsphereFullName");
        String profilePicUrl = oauthUser.getAttribute("connectsphereProfilePicUrl");

        String setupToken = pendingOAuthSignupService.create(new PendingOAuthSignup(
                Provider.valueOf(provider),
                providerId,
                email,
                fullName,
                profilePicUrl
        ));

        response.sendRedirect(setupUrl
                + "?setupToken=" + URLEncoder.encode(setupToken, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&fullName=" + URLEncoder.encode(fullName == null ? "" : fullName, StandardCharsets.UTF_8));
    }
}
