package com.animalplatform.external.publicapi;

import com.animalplatform.admin.application.ApiHealthService;
import java.net.Proxy;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PublicApiProperties.class)
public class PublicApiConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder, ApiHealthService apiHealthService) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(4));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        requestFactory.setProxy(Proxy.NO_PROXY);

        return builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    long startedAt = System.nanoTime();
                    boolean success = false;
                    try {
                        var response = execution.execute(request, body);
                        success = response.getStatusCode().is2xxSuccessful();
                        return response;
                    } finally {
                        long responseMs = (System.nanoTime() - startedAt) / 1_000_000L;
                        apiHealthService.recordPublicApiCall(responseMs, success);
                    }
                })
                .messageConverters(converters -> {
                    converters.removeIf(StringHttpMessageConverter.class::isInstance);
                    converters.add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
                })
                .build();
    }

    RestClient restClient(RestClient.Builder builder) {
        return builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
                .messageConverters(converters -> {
                    converters.removeIf(StringHttpMessageConverter.class::isInstance);
                    converters.add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
                })
                .build();
    }
}
