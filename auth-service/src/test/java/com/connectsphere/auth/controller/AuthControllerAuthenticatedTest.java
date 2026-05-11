package com.connectsphere.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectsphere.auth.dto.AuthResponse;
import com.connectsphere.auth.dto.PasswordChangeRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Authenticated endpoint tests for AuthController using @WithMockUser.
 *
 * <p>These cover the controller branches only reachable when a valid
 * {@link java.security.Principal} is present, which the basic
 * {@link AuthControllerTest} cannot exercise because all requests are
 * unauthenticated (returning 401 at the security filter).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerAuthenticatedTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private ProfilePictureStorageService profilePictureStorageService;

    // ---- GET /auth/profile --------------------------------------------------

    @Test
    @WithMockUser(username = "alice")
    void profileReturnsUserWhenAuthenticated() throws Exception {
        when(authService.getProfile("alice")).thenReturn(userResponse("alice"));

        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    // ---- POST /auth/refresh -------------------------------------------------

    @Test
    @WithMockUser(username = "alice")
    void refreshReturnsNewTokenWhenAuthenticated() throws Exception {
        when(authService.refresh("alice")).thenReturn(authResponse("new-tok", "alice"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/auth/refresh").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-tok"));
    }

    // ---- PUT /auth/profile --------------------------------------------------

    @Test
    @WithMockUser(username = "alice")
    void updateProfileReturnsUpdatedResponse() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest("alice2", "Alice Updated", null, null);
        when(authService.updateProfile(eq("alice"), any())).thenReturn(authResponse("tok", "alice2"));

        mockMvc.perform(put("/auth/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok"));
    }

    // ---- PUT /auth/password -------------------------------------------------

    @Test
    @WithMockUser(username = "alice")
    void changePasswordReturns204WhenAuthenticated() throws Exception {
        PasswordChangeRequest req = new PasswordChangeRequest("oldPass1", "newPass1");
        doNothing().when(authService).changePassword(eq("alice"), any());

        mockMvc.perform(put("/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    // ---- PUT /auth/deactivate -----------------------------------------------

    @Test
    @WithMockUser(username = "alice")
    void deactivateReturns204WhenAuthenticated() throws Exception {
        doNothing().when(authService).deactivate("alice");

        mockMvc.perform(put("/auth/deactivate").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ---- DELETE /auth/account -----------------------------------------------

    @Test
    @WithMockUser(username = "alice")
    void deleteAccountReturns204WhenAuthenticated() throws Exception {
        doNothing().when(authService).deleteAccount("alice");

        mockMvc.perform(delete("/auth/account").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ---- POST /auth/logout --------------------------------------------------

    @Test
    @WithMockUser(username = "alice")
    void logoutReturns204WhenAuthenticated() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ---- Admin endpoints with ADMIN role ------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllUsersAdminReturnsListWhenAdmin() throws Exception {
        when(authService.getAllUsers()).thenReturn(List.of(userResponse("user1")));

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user1"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void setUserStatusReturnsUpdatedUserWhenAdmin() throws Exception {
        when(authService.setUserActiveStatus(5L, false)).thenReturn(userResponse("bob"));

        mockMvc.perform(patch("/auth/admin/users/5/status").param("active", "false").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUserByAdminReturns204WhenAdmin() throws Exception {
        doNothing().when(authService).deleteUserByAdmin(5L);

        mockMvc.perform(delete("/auth/admin/users/5").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAdminAnalyticsReturnsDataWhenAdmin() throws Exception {
        AdminAnalyticsResponse analytics = new AdminAnalyticsResponse(10L, 8L, 3L, 50L, List.of());
        when(authService.getAdminAnalytics()).thenReturn(analytics);

        mockMvc.perform(get("/auth/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10));
    }

    @Test
    @WithMockUser(username = "alice", roles = {"USER"})
    void adminEndpointReturnsErrorForRegularUser() throws Exception {
        // @PreAuthorize throws AccessDeniedException which GlobalExceptionHandler
        // catches as a generic Exception → 500. Security layer returns 5xx.
        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().is5xxServerError());
    }

    // ---- Helpers ------------------------------------------------------------

    private AuthResponse authResponse(String token, String username) {
        return new AuthResponse(token, "Bearer", 86400L, userResponse(username));
    }

    private UserResponse userResponse(String username) {
        return new UserResponse(1L, username, username + "@example.com",
                "Full Name", null, null, Role.USER, Provider.LOCAL,
                null, true, Instant.now(), Instant.now());
    }
}
