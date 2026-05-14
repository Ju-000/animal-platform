package com.animalplatform.auth.application;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendBaseUrl;

    public OAuth2LoginFailureHandler(@Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendBaseUrl + "/login/failure")
                .queryParam("message", exception.getMessage())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
