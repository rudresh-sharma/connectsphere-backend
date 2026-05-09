package com.connectsphere.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.auth.client.AccountCleanupClient;
import com.connectsphere.auth.client.NotificationClient;
import com.connectsphere.auth.client.PostAdminClient;
import com.connectsphere.auth.client.SearchAdminClient;
import com.connectsphere.auth.config.AdminAccountProperties;
import com.connectsphere.auth.dto.AuthResponse;
import com.connectsphere.auth.dto.LoginRequest;
import com.connectsphere.auth.dto.PasswordChangeRequest;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UpdateProfileRequest;
import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.entity.Role;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtService;
import com.connectsphere.auth.service.AdminAnalyticsResponse;
import com.connectsphere.auth.service.ProfilePictureStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private AccountCleanupClient accountCleanupClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private PostAdminClient postAdminClient;

    @Mock
    private SearchAdminClient searchAdminClient;

    @Mock
    private ProfilePictureStorageService profilePictureStorageService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private AdminAccountProperties adminAccountProperties;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                accountCleanupClient,
                notificationClient,
                postAdminClient,
                searchAdminClient,
                profilePictureStorageService,
                jdbcTemplate,
                cacheManager,
                adminAccountProperties
        );
    }

    @Test
    void registerCreatesUserAndReturnsToken() {
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);
        RegisterRequest request = new RegisterRequest(
                "rudresh",
                "rudresh@example.com",
                "password123",
                "Rudresh Sharma"
        );
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("rudresh", response.user().username());
        verify(userRepository).save(any(User.class));
        verify(notificationClient).sendWelcomeEmail(
                eq(1L),
                eq("rudresh@example.com"),
                eq("rudresh"),
                eq("Rudresh Sharma")
        );
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "rudresh",
                "rudresh@example.com",
                "password123",
                "Rudresh Sharma"
        );
        when(userRepository.existsByUsername("rudresh")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.register(request));

        assertEquals("Username is already taken", exception.getMessage());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "rudresh",
                "rudresh@example.com",
                "password123",
                "Rudresh Sharma"
        );
        when(userRepository.existsByEmail("rudresh@example.com")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.register(request));

        assertEquals("Email is already registered", exception.getMessage());
    }

    @Test
    void loginAuthenticatesWithResolvedUsername() {
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findByEmail("rudresh@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("login-token");

        AuthResponse response = authService.login(new LoginRequest("rudresh@example.com", "secret"));

        assertEquals("login-token", response.accessToken());
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("rudresh", "secret"));
    }

    @Test
    void loginRejectsDeactivatedAccount() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        user.setActive(false);
        when(userRepository.findByEmail("rudresh@example.com")).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.login(new LoginRequest("rudresh@example.com", "secret")));

        assertEquals("Account is deactivated", exception.getMessage());
    }

    @Test
    void loginRejectsGoogleAccountPasswordSignIn() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        user.setProvider(Provider.GOOGLE);
        when(userRepository.findByEmail("rudresh@example.com")).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.login(new LoginRequest("rudresh@example.com", "secret"))
        );

        assertEquals("This account was created with Google. Please continue with Google.", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginFallsBackToUsernameLookupWhenEmailIsNotFound() {
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findByEmail("rudresh")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("username-token");

        AuthResponse response = authService.login(new LoginRequest("rudresh", "secret"));

        assertEquals("username-token", response.accessToken());
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("rudresh", "secret"));
    }

    @Test
    void refreshReturnsNewTokenForActiveUser() {
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.refresh("rudresh");

        assertEquals("refresh-token", response.accessToken());
    }

    @Test
    void updateProfileNormalizesUsernameAndClearsBlankBio() {
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newhandle")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("profile-token");

        AuthResponse response = authService.updateProfile(
                "rudresh",
                new UpdateProfileRequest(" NewHandle ", " Rudresh Sharma ", "   ", " https://img ")
        );

        assertEquals("newhandle", user.getUsername());
        assertEquals("Rudresh Sharma", user.getFullName());
        assertNull(user.getBio());
        assertEquals("https://img", user.getProfilePicUrl());
        assertEquals("profile-token", response.accessToken());
    }

    @Test
    void updateProfileRejectsSystemAdminUsernameChange() {
        when(adminAccountProperties.getUsername()).thenReturn("admin");
        User admin = activeUser(1L, "admin", "admin@example.com", Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.updateProfile("admin", new UpdateProfileRequest("other", null, null, null)));

        assertEquals("System admin username cannot be changed", exception.getMessage());
    }

    @Test
    void updateProfileRejectsDuplicateUsernameForRegularUser() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("takenname")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.updateProfile("rudresh", new UpdateProfileRequest(" takenName ", null, null, null)));

        assertEquals("Username is already taken", exception.getMessage());
    }

    @Test
    void changePasswordRequiresMatchingCurrentPassword() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        user.setPasswordHash("old-hash");
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.changePassword("rudresh", new PasswordChangeRequest("wrong", "new-pass")));

        assertEquals("Current password is incorrect", exception.getMessage());
    }

    @Test
    void changePasswordUpdatesHashWhenCurrentPasswordMatches() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        user.setPasswordHash("old-hash");
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        authService.changePassword("rudresh", new PasswordChangeRequest("correct", "new-pass"));

        assertEquals("new-hash", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateMarksRegularUserInactive() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        authService.deactivate("rudresh");

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateRejectsSystemAdmin() {
        when(adminAccountProperties.getUsername()).thenReturn("admin");
        User admin = activeUser(1L, "admin", "admin@example.com", Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.deactivate("admin"));

        assertEquals("System admin cannot be deactivated", exception.getMessage());
    }

    @Test
    void deleteAccountRemovesExternalResourcesAndUser() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        user.setProfilePicUrl("https://cdn/avatar.png");
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));

        authService.deleteAccount("rudresh");

        verify(accountCleanupClient).deleteUserData(5L);
        verify(profilePictureStorageService).deleteByUrl("https://cdn/avatar.png");
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccountRejectsSystemAdmin() {
        when(adminAccountProperties.getUsername()).thenReturn("admin");
        User admin = activeUser(1L, "admin", "admin@example.com", Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.deleteAccount("admin"));

        assertEquals("System admin cannot be deleted", exception.getMessage());
        verify(accountCleanupClient, never()).deleteUserData(any());
    }

    @Test
    void searchUsersRejectsTooShortQuery() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.searchUsers("a"));

        assertEquals("Search query must contain at least 2 characters", exception.getMessage());
    }

    @Test
    void searchUsersReturnsJdbcResultsForValidQuery() {
        Instant now = Instant.now();
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("%ru%"), eq("%ru%")))
                .thenReturn(List.of(
                        new com.connectsphere.auth.dto.UserResponse(
                                5L,
                                "rudresh",
                                "rudresh@example.com",
                                "Rudresh Sharma",
                                "bio",
                                null,
                                Role.USER,
                                Provider.LOCAL,
                                null,
                                true,
                                now,
                                now
                        )
                ));

        List<?> result = authService.searchUsers("ru");

        assertEquals(1, result.size());
    }

    @Test
    void setUserActiveStatusRejectsSystemAdminDeactivation() {
        when(adminAccountProperties.getUsername()).thenReturn("admin");
        User admin = activeUser(1L, "admin", "admin@example.com", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.setUserActiveStatus(1L, false));

        assertEquals("System admin cannot be deactivated", exception.getMessage());
    }

    @Test
    void setUserActiveStatusUpdatesRegularUser() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var response = authService.setUserActiveStatus(5L, false);

        assertFalse(user.isActive());
        assertFalse(response.active());
    }

    @Test
    void deleteUserByAdminDeletesRegularUser() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        user.setProfilePicUrl("https://cdn/avatar.png");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        authService.deleteUserByAdmin(5L);

        verify(accountCleanupClient).deleteUserData(5L);
        verify(profilePictureStorageService).deleteByUrl("https://cdn/avatar.png");
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserByAdminRejectsSystemAdmin() {
        when(adminAccountProperties.getUsername()).thenReturn("admin");
        User admin = activeUser(1L, "admin", "admin@example.com", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.deleteUserByAdmin(1L));

        assertEquals("System admin cannot be deleted", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void getAdminAnalyticsAggregatesRepositoryAndRemoteData() {
        when(userRepository.count()).thenReturn(12L);
        when(userRepository.countByIsActiveTrue()).thenReturn(10L);
        when(userRepository.countByUpdatedAtAfter(any(Instant.class))).thenReturn(4L);
        when(postAdminClient.countPosts()).thenReturn(33L);
        when(searchAdminClient.getTrendingHashtags(10)).thenReturn(List.of(
                new SearchAdminClient.HashtagSummary(1L, "java", 8),
                new SearchAdminClient.HashtagSummary(2L, "spring", 5)
        ));

        AdminAnalyticsResponse response = authService.getAdminAnalytics();

        assertEquals(12L, response.totalUsers());
        assertEquals(10L, response.activeUsers());
        assertEquals(4L, response.dailyActiveUsers());
        assertEquals(33L, response.totalPosts());
        assertEquals(2, response.trendingHashtags().size());
        assertEquals("java", response.trendingHashtags().get(0).tag());
    }

    @Test
    void getPublicUserRejectsInactiveUser() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        user.setActive(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.getPublicUser(5L));

        assertEquals("Account is deactivated", exception.getMessage());
    }

    @Test
    void getPublicUserReturnsActiveUser() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        var response = authService.getPublicUser(5L);

        assertEquals("rudresh", response.username());
        assertTrue(response.active());
    }

    @Test
    void getProfileReturnsMappedUser() {
        User user = activeUser(5L, "rudresh", "rudresh@example.com", Role.USER);
        when(userRepository.findByUsername("rudresh")).thenReturn(Optional.of(user));

        var response = authService.getProfile("rudresh");

        assertEquals("rudresh", response.username());
        assertEquals("rudresh@example.com", response.email());
    }

    @Test
    void getAllUsersReturnsNewestFirst() {
        User older = activeUser(1L, "older", "older@example.com", Role.USER);
        older.setCreatedAt(Instant.now().minusSeconds(100));
        User newer = activeUser(2L, "newer", "newer@example.com", Role.USER);
        newer.setCreatedAt(Instant.now());
        when(userRepository.findAll()).thenReturn(List.of(older, newer));

        var response = authService.getAllUsers();

        assertEquals(2, response.size());
        assertEquals("newer", response.get(0).username());
        assertEquals("older", response.get(1).username());
    }

    @Test
    void registerStillSucceedsWhenWelcomeEmailFails() {
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);
        RegisterRequest request = new RegisterRequest("anuj", "anuj@example.com", "password123", "Anuj");
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(3L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        doThrow(new RuntimeException("mail down")).when(notificationClient)
                .sendWelcomeEmail(eq(3L), eq("anuj@example.com"), eq("anuj"), eq("Anuj"));

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.accessToken());
    }

    @Test
    void refreshRejectsMissingUser() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> authService.refresh("missing"));

        assertEquals("User not found", exception.getMessage());
    }

    private static User activeUser(Long userId, String username, String email, Role role) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName("Full Name");
        user.setRole(role);
        user.setProvider(Provider.LOCAL);
        user.setPasswordHash("hashed-password");
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}
