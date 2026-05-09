package com.connectsphere.auth.security;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Handles Custom Oidc User security responsibilities.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    public CustomOidcUserService(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }
/**
 * Loads user.
 * @param userRequest method input parameter
 * @return operation result
 */

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        CustomOAuth2UserService.OAuthProvisionResult provision =
                customOAuth2UserService.provisionUser(registrationId, oidcUser.getClaims());

        Map<String, Object> claims = new LinkedHashMap<>(oidcUser.getClaims());
        customOAuth2UserService.enrichAttributes(claims, provision);

        return new DefaultOidcUser(
                oidcUser.getAuthorities(),
                userRequest.getIdToken(),
                new OidcUserInfo(claims),
                "sub"
        );
    }
}
