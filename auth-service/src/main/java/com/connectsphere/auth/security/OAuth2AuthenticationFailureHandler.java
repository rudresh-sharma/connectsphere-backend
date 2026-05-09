package com.connectsphere.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Redirects failed OAuth2 login attempts back to the frontend with an encoded error message.
 */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String failureUrl;

    public OAuth2AuthenticationFailureHandler(@Value("${app.frontend.oauth-failure-url}") String failureUrl) {
        this.failureUrl = failureUrl;
    }

    /**
     * Sends the user back to the frontend failure page when OAuth2 authentication cannot be completed.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param exception authentication failure details
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        String error = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        response.sendRedirect(failureUrl + "?error=" + error);
    }
}
