package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.AuthResponse;
import com.connectsphere.auth.dto.LoginRequest;
import com.connectsphere.auth.dto.PasswordChangeRequest;
import com.connectsphere.auth.dto.PublicUserResponse;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UpdateProfileRequest;
import com.connectsphere.auth.dto.UserResponse;
import java.util.List;

/**
 * Defines Auth business operations.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String username);

    UserResponse getProfile(String username);

    AuthResponse updateProfile(String username, UpdateProfileRequest request);

    PublicUserResponse getPublicUser(Long userId);

    void changePassword(String username, PasswordChangeRequest request);

    void deactivate(String username);

    void deleteAccount(String username);

    List<UserResponse> searchUsers(String query);

    List<UserResponse> getAllUsers();

    UserResponse setUserActiveStatus(Long userId, boolean active);

    void deleteUserByAdmin(Long userId);

    AdminAnalyticsResponse getAdminAnalytics();
}
