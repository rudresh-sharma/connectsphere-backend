package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.AuthResponse;
import com.connectsphere.auth.dto.LoginRequest;
import com.connectsphere.auth.dto.PasswordChangeRequest;
import com.connectsphere.auth.dto.ProfilePictureUploadResponse;
import com.connectsphere.auth.dto.PublicUserResponse;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UpdateProfileRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.service.AdminAnalyticsResponse;
import com.connectsphere.auth.service.AuthService;
import com.connectsphere.auth.service.ProfilePictureStorageService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
/**
 * Exposes Auth API endpoints.
 */


@RestController
@RequestMapping("/auth")

public class AuthController {

    private final AuthService authService;
    private final ProfilePictureStorageService profilePictureStorageService;

    public AuthController(AuthService authService, ProfilePictureStorageService profilePictureStorageService) {
        this.authService = authService;
        this.profilePictureStorageService = profilePictureStorageService;
    }
/**
 * Handles the register request.
 * @param request request payload
 * @return operation result
 */

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
/**
 * Handles the login request.
 * @param request request payload
 * @return operation result
 */

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
/**
 * Handles the refresh request.
 * @param principal authenticated principal
 * @return operation result
 */

    @PostMapping("/refresh")
    public AuthResponse refresh(Principal principal) {
        return authService.refresh(requirePrincipalName(principal));
    }
/**
 * Handles the logout request.
 */

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // Stateless JWT logout is handled client-side until token blacklisting is added.
    }
/**
 * Handles the profile request.
 * @param principal authenticated principal
 * @return operation result
 */

    @GetMapping("/profile")
    public UserResponse profile(Principal principal) {
        return authService.getProfile(requirePrincipalName(principal));
    }
/**
 * Updates profile.
 * @param principal authenticated principal
 * @param request request payload
 * @return operation result
 */

    @PutMapping("/profile")
    public AuthResponse updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateProfile(requirePrincipalName(principal), request);
    }
/**
 * Uploads profile picture.
 * @param principal authenticated principal
 * @param file uploaded file
 * @return operation result
 */

    @PostMapping("/profile-picture")
    public AuthResponse uploadProfilePicture(
            Principal principal,
            @RequestParam("file") MultipartFile file
    ) {
        ProfilePictureUploadResponse upload = profilePictureStorageService.upload(file);
        return authService.updateProfile(
                requirePrincipalName(principal),
                new UpdateProfileRequest(null, null, null, upload.profilePicUrl())
        );
    }
/**
 * Changes password.
 * @param principal authenticated principal
 * @param request request payload
 */

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Principal principal,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        authService.changePassword(requirePrincipalName(principal), request);
    }
/**
 * Handles the search request.
 * @param query search term
 * @return matching results
 */

    @GetMapping("/search")
    public List<UserResponse> search(@RequestParam("query") String query) {
        return authService.searchUsers(query);
    }
/**
 * Returns public user.
 * @param userId entity identifier
 * @return operation result
 */

    @GetMapping("/users/{userId}")
    public PublicUserResponse getPublicUser(@PathVariable("userId") Long userId) {
        return authService.getPublicUser(userId);
    }
/**
 * Returns internal users.
 * @return matching results
 */

    @GetMapping("/internal/users")
    public List<UserResponse> getInternalUsers() {
        return authService.getAllUsers();
    }
/**
 * Handles the deactivate request.
 * @param principal authenticated principal
 * @return HTTP response for the completed operation
 */

    @PutMapping("/deactivate")
    public ResponseEntity<Void> deactivate(Principal principal) {
        authService.deactivate(requirePrincipalName(principal));
        return ResponseEntity.noContent().build();
    }
/**
 * Deletes account.
 * @param principal authenticated principal
 * @return HTTP response for the completed operation
 */

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(Principal principal) {
        authService.deleteAccount(requirePrincipalName(principal));
        return ResponseEntity.noContent().build();
    }
/**
 * Returns all users.
 * @return matching results
 */

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return authService.getAllUsers();
    }
/**
 * Handles the user status request.
 * @param userId entity identifier
 * @param active desired active flag
 * @return resulting value
 */

    @PatchMapping("/admin/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse setUserStatus(@PathVariable("userId") Long userId, @RequestParam("active") boolean active) {
        return authService.setUserActiveStatus(userId, active);
    }
/**
 * Deletes user by admin.
 * @param userId entity identifier
 * @return HTTP response for the completed operation
 */

    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserByAdmin(@PathVariable("userId") Long userId) {
        authService.deleteUserByAdmin(userId);
        return ResponseEntity.noContent().build();
    }
/**
 * Returns admin analytics.
 * @return operation result
 */

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminAnalyticsResponse getAdminAnalytics() {
        return authService.getAdminAnalytics();
    }

    private String requirePrincipalName(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal.getName();
    }
}
