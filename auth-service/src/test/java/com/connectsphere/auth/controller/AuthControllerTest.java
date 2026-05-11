package com.connectsphere.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectsphere.auth.dto.AuthResponse;
import com.connectsphere.auth.dto.LoginRequest;
import com.connectsphere.auth.dto.PasswordChangeRequest;
import com.connectsphere.auth.dto.PublicUserResponse;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UpdateProfileRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.entity.Role;
import com.connectsphere.auth.service.AdminAnalyticsResponse;
import com.connectsphere.auth.service.AuthService;
import com.connectsphere.auth.service.ProfilePictureStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller-layer integration tests for AuthController.
 *
 * <p>Uses a full Spring Boot context with MockMvc. Most authenticated endpoints
 * are exercised unauthenticated to verify the controller's own 401 guard
 * (requirePrincipalName). Public endpoints are exercised normally.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private ProfilePictureStorageService profilePictureStorageService;

    // ---- POST /auth/register ------------------------------------------------

    @Test
    void registerReturns201WithToken() throws Exception {
        RegisterRequest req = new RegisterRequest("alice123", "alice@example.com", "password1", "Alice");
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse("tok-alice", "alice123"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("tok-alice"))
                .andExpect(jsonPath("$.user.username").value("alice123"));
    }

    // ---- POST /auth/login ---------------------------------------------------

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        LoginRequest req = new LoginRequest("alice@example.com", "password1");
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse("tok-login", "alice123"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok-login"));
    }

    // ---- POST /auth/logout --------------------------------------------------
    // /auth/** requires authentication (SecurityConfig); unauthenticated → 401

    @Test
    void logoutReturnsWith401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET /auth/profile ------------------------------------------------
    // No principal → requirePrincipalName → 401

    @Test
    void profileReturnsWith401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ---- PUT /auth/profile --------------------------------------------------

    @Test
    void updateProfileReturnsWith401WhenNotAuthenticated() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest("newname", null, null, null);
        mockMvc.perform(put("/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ---- PUT /auth/password -------------------------------------------------

    @Test
    void changePasswordReturnsWith401WhenNotAuthenticated() throws Exception {
        PasswordChangeRequest req = new PasswordChangeRequest("old", "newPass1");
        mockMvc.perform(put("/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ---- PUT /auth/deactivate -----------------------------------------------

    @Test
    void deactivateReturnsWith401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/auth/deactivate"))
                .andExpect(status().isUnauthorized());
    }

    // ---- DELETE /auth/account -----------------------------------------------

    @Test
    void deleteAccountReturnsWith401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/auth/account"))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET /auth/search ---------------------------------------------------

    @Test
    void searchReturnsUserList() throws Exception {
        when(authService.searchUsers("alice")).thenReturn(List.of(userResponse("alice")));

        mockMvc.perform(get("/auth/search").param("query", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    // ---- GET /auth/users/{userId} -------------------------------------------

    @Test
    void getPublicUserReturnsResponse() throws Exception {
        PublicUserResponse pub = new PublicUserResponse(5L, "alice", "Alice", null, null,
                "alice@example.com", Role.USER, true, Instant.now(), Instant.now());
        when(authService.getPublicUser(5L)).thenReturn(pub);

        mockMvc.perform(get("/auth/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    // ---- GET /auth/internal/users -------------------------------------------

    @Test
    void getInternalUsersReturnsList() throws Exception {
        when(authService.getAllUsers()).thenReturn(List.of(userResponse("alice")));

        mockMvc.perform(get("/auth/internal/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    // ---- Helpers ------------------------------------------------------------

    private AuthResponse authResponse(String token, String username) {
        return new AuthResponse(token, "Bearer", 86400L, userResponse(username));
    }

    private UserResponse userResponse(String username) {
        return new UserResponse(
                1L, username, username + "@example.com", "Full Name",
                null, null, Role.USER, Provider.LOCAL, null,
                true, Instant.now(), Instant.now()
        );
    }
}
