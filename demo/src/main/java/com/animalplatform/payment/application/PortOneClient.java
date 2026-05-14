package com.animalplatform.payment.application;

import com.animalplatform.payment.config.PortOneProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PortOneClient {

    private static final Logger log = LoggerFactory.getLogger(PortOneClient.class);

    private final RestClient restClient;
    private final PortOneProperties properties;

    public PortOneClient(RestClient.Builder builder, PortOneProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(resolveBaseUrl(properties.getApiBaseUrl()))
                .requestFactory(portOneRequestFactory())
                .build();
    }

    public PortOnePayment getPayment(String impUid) {
        String accessToken = getAccessToken();
        PortOneResponse response = exchange("payment_lookup", () -> restClient.get()
                .uri("/payments/{impUid}", impUid)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .retrieve()
                .body(PortOneResponse.class));

        Map<String, Object> payment = requireResponseMap(response, "PortOne payment lookup failed.");
        return new PortOnePayment(
                text(payment.get("imp_uid")),
                text(payment.get("merchant_uid")),
                decimal(payment.get("amount")),
                text(payment.get("status"))
        );
    }

    public void cancelPayment(String impUid, String merchantUid, String reason) {
        try {
            String accessToken = getAccessToken();
            exchange("payment_cancel", () -> restClient.post()
                    .uri("/payments/cancel")
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .body(Map.of(
                            "imp_uid", impUid,
                            "merchant_uid", merchantUid,
                            "reason", reason
                    ))
                    .retrieve()
                    .body(PortOneResponse.class));
            log.info("PortOne payment cancel requested. impUid={}, merchantUid={}, reason={}", impUid, merchantUid, reason);
        } catch (ResponseStatusException exception) {
            log.warn("PortOne payment cancel failed. impUid={}, merchantUid={}, status={}",
                    impUid, merchantUid, exception.getStatusCode());
        }
    }

    private String getAccessToken() {
        if (!StringUtils.hasText(properties.getApiKey()) || !StringUtils.hasText(properties.getApiSecret())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PortOne API credentials are not configured."
            );
        }

        PortOneResponse response = exchange("token", () -> restClient.post()
                .uri("/users/getToken")
                .body(Map.of(
                        "imp_key", properties.getApiKey(),
                        "imp_secret", properties.getApiSecret()
                ))
                .retrieve()
                .body(PortOneResponse.class));

        Map<String, Object> token = requireResponseMap(response, "PortOne token response is invalid.");
        String accessToken = text(token.get("access_token"));
        if (!StringUtils.hasText(accessToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne access token is missing.");
        }
        return accessToken;
    }

    private PortOneResponse exchange(String operation, PortOneRequest request) {
        try {
            PortOneResponse response = request.execute();
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne response is empty.");
            }
            if (response.code() != 0) {
                log.warn("PortOne API returned an error. operation={}, code={}, message={}",
                        operation, response.code(), response.message());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne API request failed.");
            }
            return response;
        } catch (ResourceAccessException exception) {
            log.warn("PortOne API timed out or is unreachable. operation={}", operation);
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "PortOne API timeout.", exception);
        } catch (RestClientException exception) {
            log.warn("PortOne API request error. operation={}, error={}", operation, exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne API request failed.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireResponseMap(PortOneResponse response, String message) {
        if (response.response() instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    private ClientHttpRequestFactory portOneRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    private String resolveBaseUrl(String baseUrl) {
        return StringUtils.hasText(baseUrl) ? baseUrl : "https://api.iamport.kr";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne amount is invalid.");
        }
    }

    private interface PortOneRequest {
        PortOneResponse execute();
    }

    private record PortOneResponse(int code, String message, Object response) {
    }

    public record PortOnePayment(String impUid, String merchantUid, BigDecimal amount, String status) {
    }
}
