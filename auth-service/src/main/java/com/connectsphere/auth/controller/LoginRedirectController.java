package com.connectsphere.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
/**
 * Exposes Login Redirect API endpoints.
 */


@Controller

public class LoginRedirectController {

    private final String frontendLoginUrl;

    public LoginRedirectController(@Value("${app.frontend.oauth-failure-url}") String frontendLoginUrl) {
        this.frontendLoginUrl = frontendLoginUrl;
    }
/**
 * Handles the login request.
 * @return operation result
 */

    @GetMapping("/login")
    public String login() {
        return "redirect:" + frontendLoginUrl;
    }
/**
 * Handles the oauth 2login request.
 * @return resulting value
 */

    @GetMapping("/auth/oauth2-login")
    public String oauth2Login() {
        return "redirect:/oauth2/authorization/google";
    }
}
