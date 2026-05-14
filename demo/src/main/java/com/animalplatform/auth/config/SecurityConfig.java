package com.animalplatform.auth.config;

import com.animalplatform.auth.application.CustomOAuth2UserService;
import com.animalplatform.auth.application.OAuth2LoginFailureHandler;
import com.animalplatform.auth.application.OAuth2LoginSuccessHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2LoginSuccessHandler successHandler,
            OAuth2LoginFailureHandler failureHandler
    ) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");

        http
                // This app uses session cookies for local and OAuth login, so CSRF must stay enabled
                // for browser state-changing requests. The token is sent as an XSRF-TOKEN cookie and
                // the React client echoes it with the X-XSRF-TOKEN header on POST/PUT/PATCH/DELETE.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                // API auth is handled by explicit JSON login and OAuth2 login, not HTTP Basic or form login.
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeHttpRequests(auth -> auth
                        // Only low-risk actuator endpoints are public. Sensitive endpoints are either
                        // not exposed by management.endpoints.web.exposure or require ADMIN if enabled later.
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/stories", "/api/stories/*", "/uploads/**").permitAll()
                        .requestMatchers(
                                "/",
                                "/error",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/api/csrf",
                                "/api/auth/**",
                                "/api/campaigns",
                                "/api/chat",
                                "/api/adoptions",
                                "/api/adoptions/checklist",
                                "/api/animals/**",
                                "/api/matching",
                                "/api/shelters/**",
                                "/api/donations/options",
                                "/api/donations/stats",
                                "/api/stats/**",
                                "/api/payments/portone/config",
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/me", "/api/favorites/**", "/api/payments/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Security headers: no MIME sniffing, no framing outside the disabled local H2 profile,
                // and limited referrer leakage for cross-origin navigation.
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                        ))
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> response.sendError(HttpStatus.UNAUTHORIZED.value()),
                                request -> request.getRequestURI().startsWith("/api/")
                        )
                )
                // Force creation of the CSRF cookie on safe requests so the SPA can read and echo it.
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                )
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .oauth2Client(Customizer.withDefaults());

        return http.build();
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
