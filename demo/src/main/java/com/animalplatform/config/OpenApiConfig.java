package com.animalplatform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SESSION_COOKIE_AUTH = "sessionCookieAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("포동포동 API")
                        .description("유기동물 보호/입양/후원 플랫폼 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("포동포동 개발팀")
                                .email("contact@example.com")
                                .url("https://example.com")))
                .components(new Components()
                        .addSecuritySchemes(SESSION_COOKIE_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("세션 쿠키 기반 인증")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE_AUTH));
    }
}
