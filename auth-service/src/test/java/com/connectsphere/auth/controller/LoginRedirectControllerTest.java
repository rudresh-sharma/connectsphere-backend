package com.connectsphere.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectsphere.auth.service.AuthService;
import com.connectsphere.auth.service.ProfilePictureStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for LoginRedirectController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginRedirectControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private AuthService authService;
    @MockBean  private ProfilePictureStorageService profilePictureStorageService;

    @Test
    void loginEndpointRedirectsToFrontend() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void oauth2LoginEndpointRedirectsToFrontend() throws Exception {
        mockMvc.perform(get("/auth/oauth2-login"))
                .andExpect(status().is3xxRedirection());
    }
}
