package com.animalplatform.auth.application;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> normalized = normalize(registrationId, oauth2User.getAttributes());

        Collection<? extends GrantedAuthority> authorities =
                Set.of(new SimpleGrantedAuthority("ROLE_MEMBER"));

        return new DefaultOAuth2User(authorities, normalized, "id");
    }

    private Map<String, Object> normalize(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> normalizeGoogle(attributes);
            case "kakao" -> normalizeKakao(attributes);
            case "naver" -> normalizeNaver(attributes);
            default -> normalizeDefault(registrationId, attributes);
        };
    }

    private Map<String, Object> normalizeGoogle(Map<String, Object> attributes) {
        Map<String, Object> normalized = baseAttributes("google", attributes.get("sub"));
        normalized.put("name", attributes.get("name"));
        normalized.put("email", attributes.get("email"));
        normalized.put("picture", attributes.get("picture"));
        normalized.put("rawAttributes", attributes);
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeKakao(Map<String, Object> attributes) {
        Map<String, Object> normalized = baseAttributes("kakao", attributes.get("id"));
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

        normalized.put("name", profile.get("nickname"));
        normalized.put("email", kakaoAccount.get("email"));
        normalized.put("picture", profile.get("profile_image_url"));
        normalized.put("rawAttributes", attributes);
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.getOrDefault("response", Map.of());
        Map<String, Object> normalized = baseAttributes("naver", response.get("id"));
        normalized.put("name", response.get("name"));
        normalized.put("email", response.get("email"));
        normalized.put("picture", response.get("profile_image"));
        normalized.put("rawAttributes", attributes);
        return normalized;
    }

    private Map<String, Object> normalizeDefault(String registrationId, Map<String, Object> attributes) {
        Map<String, Object> normalized = baseAttributes(registrationId, attributes.get("id"));
        normalized.put("name", attributes.get("name"));
        normalized.put("email", attributes.get("email"));
        normalized.put("picture", attributes.get("picture"));
        normalized.put("rawAttributes", attributes);
        return normalized;
    }

    private Map<String, Object> baseAttributes(String provider, Object id) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("provider", provider);
        normalized.put("id", String.valueOf(id));
        return normalized;
    }
}
