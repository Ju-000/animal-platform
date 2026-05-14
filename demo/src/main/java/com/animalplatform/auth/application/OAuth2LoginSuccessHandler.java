package com.animalplatform.auth.application;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final String frontendBaseUrl;

    public OAuth2LoginSuccessHandler(@Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User user = token.getPrincipal();

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendBaseUrl + "/login/success")
                .queryParam("provider", token.getAuthorizedClientRegistrationId())
                .queryParam("id", String.valueOf(user.getAttribute("id")))
                .queryParam("name", String.valueOf(user.getAttribute("name")))
                .queryParam("email", String.valueOf(user.getAttribute("email")))
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
