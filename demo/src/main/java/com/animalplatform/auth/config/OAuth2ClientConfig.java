package com.animalplatform.auth.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(SocialLoginProperties.class)
public class OAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(SocialLoginProperties properties) {
        List<ClientRegistration> registrations = new ArrayList<>();

        if (isConfigured(properties.getGoogle())) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(properties.getGoogle().getClientId())
                    .clientSecret(properties.getGoogle().getClientSecret())
                    .scope("openid", "profile", "email")
                    .build());
        }

        if (isConfigured(properties.getKakao())) {
            registrations.add(ClientRegistration.withRegistrationId("kakao")
                    .clientId(properties.getKakao().getClientId())
                    .clientSecret(properties.getKakao().getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("profile_nickname", "account_email")
                    .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                    .tokenUri("https://kauth.kakao.com/oauth/token")
                    .userInfoUri("https://kapi.kakao.com/v2/user/me")
                    .userNameAttributeName("id")
                    .clientName("Kakao")
                    .build());
        }

        if (isConfigured(properties.getNaver())) {
            registrations.add(ClientRegistration.withRegistrationId("naver")
                    .clientId(properties.getNaver().getClientId())
                    .clientSecret(properties.getNaver().getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("name", "email")
                    .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                    .tokenUri("https://nid.naver.com/oauth2.0/token")
                    .userInfoUri("https://openapi.naver.com/v1/nid/me")
                    .userNameAttributeName("response")
                    .clientName("Naver")
                    .build());
        }

        if (registrations.isEmpty()) {
            return registrationId -> null;
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repository) {
        return new InMemoryOAuth2AuthorizedClientService(repository);
    }

    private boolean isConfigured(SocialLoginProperties.Provider provider) {
        return StringUtils.hasText(provider.getClientId()) && StringUtils.hasText(provider.getClientSecret());
    }
}
