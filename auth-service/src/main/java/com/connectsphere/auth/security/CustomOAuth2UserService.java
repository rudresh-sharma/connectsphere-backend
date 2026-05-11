package com.connectsphere.auth.security;

import com.connectsphere.auth.entity.Provider;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads OAuth2 user details and aligns provider identities with local ConnectSphere accounts.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the provider profile and enriches it with local account metadata for downstream handlers.
     *
     * @param userRequest OAuth2 provider user request
     * @return normalized OAuth2 user representation
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvisionResult provision = provisionUser(registrationId, oauthUser.getAttributes());
        Map<String, Object> attributes = new LinkedHashMap<>(oauthUser.getAttributes());
        enrichAttributes(attributes, provision);

        return new DefaultOAuth2User(
                Set.copyOf(oauthUser.getAuthorities()),
                attributes,
                userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
        );
    }

    /**
     * Finds or prepares the local user record associated with the provider profile.
     *
     * @param registrationId OAuth provider registration identifier
     * @param attributes raw provider attributes
     * @return provisioning result for the authenticated provider user
     */
    public OAuthProvisionResult provisionUser(String registrationId, Map<String, Object> attributes) {
        OAuthProfile profile = mapProfile(registrationId, attributes);

        return userRepository.findByEmail(profile.email())
                .map(existing -> new OAuthProvisionResult(userRepository.saveAndFlush(updateExistingUser(existing, profile)), profile, true))
                .orElseGet(() -> new OAuthProvisionResult(null, profile, false));
    }

    void enrichAttributes(Map<String, Object> attributes, OAuthProvisionResult provision) {
        attributes.put("connectsphereExistingUser", provision.existingUser());
        attributes.put("connectsphereEmail", provision.profile().email());
        attributes.put("connectsphereProvider", provision.profile().provider().name());
        attributes.put("connectsphereProviderId", provision.profile().providerId());
        attributes.put("connectsphereFullName", provision.profile().fullName());
        attributes.put("connectsphereProfilePicUrl", provision.profile().profilePicUrl());

        if (provision.user() != null) {
            attributes.put("connectsphereUserId", provision.user().getUserId());
        }
    }

    private OAuthProfile mapProfile(String registrationId, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return new OAuthProfile(
                    Provider.GOOGLE,
                    value(attributes.get("sub")),
                    value(attributes.get("email")),
                    value(attributes.get("name")),
                    value(attributes.get("picture")),
                    null
            );
        }

        if ("github".equalsIgnoreCase(registrationId)) {
            String email = value(attributes.get("email"));
            if (email == null || email.isBlank()) {
                throw new OAuth2AuthenticationException("GitHub account email is private or unavailable");
            }
            return new OAuthProfile(
                    Provider.GITHUB,
                    value(attributes.get("id")),
                    email,
                    valueOrFallback(attributes.get("name"), value(attributes.get("login"))),
                    value(attributes.get("avatar_url")),
                    value(attributes.get("login"))
            );
        }

        throw new OAuth2AuthenticationException("Unsupported OAuth provider: " + registrationId);
    }

    private User updateExistingUser(User user, OAuthProfile profile) {
        user.setProvider(profile.provider());
        user.setProviderId(profile.providerId());
        if (user.getProfilePicUrl() == null || user.getProfilePicUrl().isBlank()) {
            user.setProfilePicUrl(profile.profilePicUrl());
        }
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(profile.fullName());
        }
        user.setActive(true);
        return user;
    }

    private String value(Object object) {
        return object == null ? null : object.toString();
    }

    private String valueOrFallback(Object object, String fallback) {
        String value = value(object);
        return value == null || value.isBlank() ? fallback : value;
    }

    public record OAuthProvisionResult(User user, OAuthProfile profile, boolean existingUser) {
    }

    public record OAuthProfile(
            Provider provider,
            String providerId,
            String email,
            String fullName,
            String profilePicUrl,
            String usernameHint
    ) {
    }
}
