package com.connectsphere.auth.service.impl;

import com.connectsphere.auth.client.AccountCleanupClient;
import com.connectsphere.auth.client.NotificationClient;
import com.connectsphere.auth.client.PostAdminClient;
import com.connectsphere.auth.client.SearchAdminClient;
import com.connectsphere.auth.config.AdminAccountProperties;
import com.connectsphere.auth.config.CacheConfig;
import com.connectsphere.auth.dto.AuthResponse;
import com.connectsphere.auth.dto.LoginRequest;
import com.connectsphere.auth.dto.PasswordChangeRequest;
import com.connectsphere.auth.dto.PublicUserResponse;
import com.connectsphere.auth.dto.RegisterRequest;
import com.connectsphere.auth.dto.UpdateProfileRequest;
import com.connectsphere.auth.dto.UserResponse;
import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.entity.Role;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtService;
import com.connectsphere.auth.service.AdminAnalyticsResponse;
import com.connectsphere.auth.service.AuthService;
import com.connectsphere.auth.util.UserMapper;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Implements Auth business operations.
 */


@Service
@Transactional

public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountCleanupClient accountCleanupClient;
    private final NotificationClient notificationClient;
    private final PostAdminClient postAdminClient;
    private final SearchAdminClient searchAdminClient;
    private final com.connectsphere.auth.service.ProfilePictureStorageService profilePictureStorageService;
    private final JdbcTemplate jdbcTemplate;
    private final CacheManager cacheManager;
    private final AdminAccountProperties adminAccountProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AccountCleanupClient accountCleanupClient,
            NotificationClient notificationClient,
            PostAdminClient postAdminClient,
            SearchAdminClient searchAdminClient,
            com.connectsphere.auth.service.ProfilePictureStorageService profilePictureStorageService,
            JdbcTemplate jdbcTemplate,
            CacheManager cacheManager,
            AdminAccountProperties adminAccountProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accountCleanupClient = accountCleanupClient;
        this.notificationClient = notificationClient;
        this.postAdminClient = postAdminClient;
        this.searchAdminClient = searchAdminClient;
        this.profilePictureStorageService = profilePictureStorageService;
        this.jdbcTemplate = jdbcTemplate;
        this.cacheManager = cacheManager;
        this.adminAccountProperties = adminAccountProperties;
    }
/**
 * Performs the register operation.
 * @param request request payload
 * @return operation result
 */

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setBio(null);
        user.setRole(Role.USER);
        user.setProvider(Provider.LOCAL);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        sendWelcomeEmail(savedUser);
        return tokenResponse(savedUser);
    }
/**
 * Performs the login operation.
 * @param request request payload
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = findByEmailOrUsername(request.emailOrUsername());
        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }
        if (user.getProvider() != Provider.LOCAL) {
            throw new BadRequestException(providerLoginMessage(user.getProvider()));
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
        );

        return tokenResponse(user);
    }
/**
 * Performs the refresh operation.
 * @param username method input parameter
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String username) {
        return tokenResponse(findActiveByUsername(username));
    }

    private String providerLoginMessage(Provider provider) {
        return switch (provider) {
            case GOOGLE -> "This account was created with Google. Please continue with Google.";
            case GITHUB -> "This account was created with GitHub. Please continue with GitHub.";
            default -> "Please sign in with the provider used to create this account.";
        };
    }
/**
 * Returns profile.
 * @param username method input parameter
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.USER_PROFILES_CACHE, key = "#p0")
    public UserResponse getProfile(String username) {
        return UserMapper.toResponse(findActiveByUsername(username));
    }
/**
 * Updates profile.
 * @param username method input parameter
 * @param request request payload
 * @return operation result
 */

    @Override
    public AuthResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = findActiveByUsername(username);
        String previousUsername = user.getUsername();
        if (request.username() != null && !request.username().isBlank()) {
            String normalizedUsername = request.username().trim().toLowerCase(Locale.ROOT);
            if (isSystemAdmin(user) && !normalizedUsername.equals(user.getUsername())) {
                throw new BadRequestException("System admin username cannot be changed");
            }
            if (!normalizedUsername.equals(user.getUsername()) && userRepository.existsByUsername(normalizedUsername)) {
                throw new BadRequestException("Username is already taken");
            }
            user.setUsername(normalizedUsername);
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.bio() != null) {
            String bio = request.bio().trim();
            user.setBio(bio.isEmpty() ? null : bio);
        }
        if (request.profilePicUrl() != null) {
            user.setProfilePicUrl(request.profilePicUrl().trim());
        }
        User savedUser = userRepository.save(user);
        evictUserCaches(savedUser, previousUsername);
        return tokenResponse(savedUser);
    }
/**
 * Returns public user.
 * @param userId entity identifier
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    public PublicUserResponse getPublicUser(Long userId) {
        return UserMapper.toPublicResponse(findActiveByUserId(userId));
    }
/**
 * Changes password.
 * @param username method input parameter
 * @param request request payload
 */

    @Override
    public void changePassword(String username, PasswordChangeRequest request) {
        User user = findActiveByUsername(username);
        if (isSystemAdmin(user)) {
            throw new BadRequestException("System admin password is managed by configuration");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        evictUserCaches(userRepository.save(user));
    }
/**
 * Performs the deactivate operation.
 * @param username method input parameter
 */

    @Override
    public void deactivate(String username) {
        User user = findActiveByUsername(username);
        if (isSystemAdmin(user)) {
            throw new BadRequestException("System admin cannot be deactivated");
        }
        user.setActive(false);
        evictUserCaches(userRepository.save(user));
    }
/**
 * Deletes account.
 * @param username method input parameter
 */

    @Override
    public void deleteAccount(String username) {
        User user = findActiveByUsername(username);
        if (isSystemAdmin(user)) {
            throw new BadRequestException("System admin cannot be deleted");
        }
        accountCleanupClient.deleteUserData(user.getUserId());
        profilePictureStorageService.deleteByUrl(user.getProfilePicUrl());
        userRepository.delete(user);
        evictUserCaches(user);
    }
/**
 * Searches users.
 * @param query search term
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String query) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.length() < 2) {
            throw new BadRequestException("Search query must contain at least 2 characters");
        }
        String pattern = "%" + safeQuery.toLowerCase(Locale.ROOT) + "%";
        return jdbcTemplate.query(
                """
                SELECT user_id, username, email, full_name, bio, profile_pic_url, role,
                       provider, provider_id, is_active, created_at, updated_at
                FROM users
                WHERE is_active = 1
                  AND (LOWER(username) LIKE ? OR LOWER(full_name) LIKE ?)
                ORDER BY username ASC
                LIMIT 20
                """,
                (rs, rowNum) -> new UserResponse(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("bio"),
                        rs.getString("profile_pic_url"),
                        Role.valueOf(rs.getString("role")),
                        Provider.valueOf(rs.getString("provider")),
                        rs.getString("provider_id"),
                        rs.getBoolean("is_active"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                pattern,
                pattern
        );
    }
/**
 * Returns all users.
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(UserMapper::toResponse)
                .toList();
    }
/**
 * Sets user active status.
 * @param userId entity identifier
 * @param active desired active flag
 * @return resulting value
 */

    @Override
    public UserResponse setUserActiveStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        if (isSystemAdmin(user) && !active) {
            throw new BadRequestException("System admin cannot be deactivated");
        }
        user.setActive(active);
        User savedUser = userRepository.save(user);
        evictUserCaches(savedUser);
        return UserMapper.toResponse(savedUser);
    }
/**
 * Deletes user by admin.
 * @param userId entity identifier
 */

    @Override
    public void deleteUserByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        if (isSystemAdmin(user)) {
            throw new BadRequestException("System admin cannot be deleted");
        }
        accountCleanupClient.deleteUserData(user.getUserId());
        profilePictureStorageService.deleteByUrl(user.getProfilePicUrl());
        userRepository.delete(user);
        evictUserCaches(user);
    }
/**
 * Returns admin analytics.
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminAnalytics() {
        List<AdminAnalyticsResponse.TrendingHashtagSummary> trendingHashtags = searchAdminClient.getTrendingHashtags(10).stream()
                .map(hashtag -> new AdminAnalyticsResponse.TrendingHashtagSummary(
                        hashtag.hashtagId(),
                        hashtag.tag(),
                        hashtag.postCount()
                ))
                .toList();

        return new AdminAnalyticsResponse(
                userRepository.count(),
                userRepository.countByIsActiveTrue(),
                userRepository.countByUpdatedAtAfter(Instant.now().minusSeconds(86400)),
                postAdminClient.countPosts(),
                trendingHashtags
        );
    }

    private User findByEmailOrUsername(String emailOrUsername) {
        String value = emailOrUsername.trim();
        return userRepository.findByEmail(value.toLowerCase())
                .or(() -> userRepository.findByUsername(value))
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
    }

    private User findActiveByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }
        return user;
    }

    private User findActiveByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }
        return user;
    }

    private AuthResponse tokenResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), UserMapper.toResponse(user));
    }

    private boolean isSystemAdmin(User user) {
        return user.getRole() == Role.ADMIN
                && user.getUsername() != null
                && user.getUsername().equals(normalizedAdminUsername());
    }

    private String normalizedAdminUsername() {
        return adminAccountProperties.getUsername().trim().toLowerCase(Locale.ROOT);
    }

    private void sendWelcomeEmail(User user) {
        try {
            notificationClient.sendWelcomeEmail(
                    user.getUserId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getFullName()
            );
        } catch (Exception ex) {
            log.warn("Could not send welcome email for userId={}: {}", user.getUserId(), ex.getMessage());
        }
    }

    private void evictUserCaches(User user) {
        evictUserCaches(user, user.getUsername());
    }

    private void evictUserCaches(User user, String previousUsername) {
        Cache userProfiles = cacheManager.getCache(CacheConfig.USER_PROFILES_CACHE);
        if (userProfiles != null) {
            userProfiles.evictIfPresent(user.getUsername());
            if (previousUsername != null && !previousUsername.equals(user.getUsername())) {
                userProfiles.evictIfPresent(previousUsername);
            }
        }

        Cache publicUsers = cacheManager.getCache(CacheConfig.PUBLIC_USERS_CACHE);
        if (publicUsers != null) {
            publicUsers.evictIfPresent(user.getUserId());
        }
    }
}
