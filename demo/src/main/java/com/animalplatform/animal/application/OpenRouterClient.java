package com.animalplatform.animal.application;

import com.animalplatform.auth.application.AuthenticatedUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1_000L;
    private static final String PRIMARY_KOREAN_FREE_MODEL = "meta-llama/llama-3.3-70b-instruct:free";
    private static final String KOREAN_ONLY_INSTRUCTION = """
            IMPORTANT: Respond in Korean (한국어) ONLY.
            No English, No Chinese, No Spanish, No German, No Japanese.
            반드시 순수한 한국어로만 답변하세요.
            """;
    private static final String KOREAN_ONLY_USER_PREFIX = "(한국어로만 답변) ";

    private final OpenRouterProperties openRouterProperties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<AuthenticatedUserService> authenticatedUserServiceProvider;
    private final RestClient restClient;

    public OpenRouterClient(
            OpenRouterProperties openRouterProperties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ObjectProvider<AuthenticatedUserService> authenticatedUserServiceProvider
    ) {
        this.openRouterProperties = openRouterProperties;
        this.objectMapper = objectMapper;
        this.authenticatedUserServiceProvider = authenticatedUserServiceProvider;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        requestFactory.setProxy(Proxy.NO_PROXY);

        this.restClient = restClientBuilder
                .baseUrl(openRouterProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    public String callSummary(String systemPrompt, String userPrompt, int maxTokens) {
        return createChatCompletion("summary", systemPrompt, List.of(Map.of(
                "role", "user",
                "content", userPrompt
        )), maxTokens);
    }

    public String callChat(String systemPrompt, List<Map<String, String>> messages, int maxTokens) {
        return createChatCompletion("chat", systemPrompt, messages, maxTokens);
    }

    private String createChatCompletion(String endpoint, String systemPrompt, List<Map<String, String>> messages, int maxTokens) {
        if (!StringUtils.hasText(openRouterProperties.getApiKey())) {
            logCall(endpoint, 0, false, "not_configured", "none");
            throw new OpenRouterApiException(endpoint, HttpStatus.SERVICE_UNAVAILABLE.value(), "OpenRouter API key is not configured.");
        }

        List<Map<String, String>> normalizedMessages = normalizeMessages(systemPrompt, messages);
        if (normalizedMessages.stream().noneMatch(message -> "user".equals(message.get("role")))) {
            throw new OpenRouterApiException(endpoint, HttpStatus.BAD_REQUEST.value(), "OpenRouter messages must contain a user message.");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("messages", normalizedMessages);
        requestBody.put("max_tokens", Math.max(maxTokens, 1_000));

        OpenRouterApiException lastException = null;

        for (String model : candidateModels()) {
            requestBody.put("model", model);
            logRequestBody(requestBody);

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                long startedAt = System.nanoTime();
                try {
                    String text = postChatCompletion(endpoint, requestBody);
                    logCall(endpoint, elapsedMs(startedAt), true, "200", model);
                    return text;
                } catch (OpenRouterApiException exception) {
                    lastException = exception;
                    logCall(endpoint, elapsedMs(startedAt), false, String.valueOf(exception.getStatusCode()), model);
                    if (!shouldRetry(exception.getStatusCode())) {
                        throw exception;
                    }
                    if (attempt == MAX_ATTEMPTS) {
                        log.warn("OpenRouter model exhausted. endpoint={} model={} status={}", endpoint, model, exception.getStatusCode());
                        break;
                    }
                } catch (RestClientException exception) {
                    lastException = new OpenRouterApiException(endpoint, HttpStatus.BAD_GATEWAY.value(), "Failed to call OpenRouter API.", exception);
                    logCall(endpoint, elapsedMs(startedAt), false, "network_error", model);
                    if (attempt == MAX_ATTEMPTS) {
                        log.warn("OpenRouter model exhausted after network errors. endpoint={} model={}", endpoint, model);
                        break;
                    }
                }
                sleepBeforeRetry(endpoint, attempt);
            }
        }

        throw lastException == null
                ? new OpenRouterApiException(endpoint, HttpStatus.BAD_GATEWAY.value(), "Failed to call OpenRouter API.")
                : lastException;
    }

    private String postChatCompletion(String endpoint, Map<String, Object> requestBody) {
        return restClient.post()
                .uri("/api/v1/chat/completions")
                .headers(headers -> {
                    headers.setBearerAuth(openRouterProperties.getApiKey());
                    if (StringUtils.hasText(openRouterProperties.getReferer())) {
                        headers.set("HTTP-Referer", openRouterProperties.getReferer());
                    }
                    if (StringUtils.hasText(openRouterProperties.getTitle())) {
                        headers.set("X-Title", openRouterProperties.getTitle());
                    }
                })
                .body(requestBody)
                .exchange((request, response) -> {
                    HttpStatusCode statusCode = response.getStatusCode();
                    String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    if (!statusCode.is2xxSuccessful()) {
                        log.warn("OpenRouter error response. endpoint={} status={} body={}", endpoint, statusCode.value(), responseBody);
                        throw new OpenRouterApiException(endpoint, statusCode.value(), "OpenRouter API returned HTTP " + statusCode.value());
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
                    String text = extractText(body);
                    if (!StringUtils.hasText(text)) {
                        throw new OpenRouterApiException(endpoint, HttpStatus.BAD_GATEWAY.value(), "OpenRouter response did not contain text.");
                    }
                    return text.trim();
                });
    }

    private List<Map<String, String>> normalizeMessages(String systemPrompt, List<Map<String, String>> messages) {
        List<Map<String, String>> normalized = new ArrayList<>();
        normalized.add(Map.of(
                "role", "system",
                "content", koreanOnlySystemPrompt(systemPrompt)
        ));

        if (messages == null) {
            return normalized;
        }

        for (Map<String, String> message : messages) {
            String role = toOpenRouterRole(message.get("role"));
            String content = message.get("content");
            if (!StringUtils.hasText(role) || !StringUtils.hasText(content)) {
                continue;
            }
            if (normalized.size() == 1 && "assistant".equals(role)) {
                continue;
            }

            Map<String, String> normalizedMessage = Map.of(
                    "role", role,
                    "content", "user".equals(role) ? koreanOnlyUserContent(content) : content
            );
            if (!normalized.isEmpty()
                    && !"system".equals(role)
                    && role.equals(normalized.getLast().get("role"))) {
                normalized.set(normalized.size() - 1, normalizedMessage);
                continue;
            }
            normalized.add(normalizedMessage);
        }

        return normalized;
    }

    private String koreanOnlySystemPrompt(String systemPrompt) {
        if (!StringUtils.hasText(systemPrompt)) {
            return KOREAN_ONLY_INSTRUCTION.trim();
        }
        if (systemPrompt.startsWith("IMPORTANT: Respond in Korean")) {
            return systemPrompt;
        }
        return KOREAN_ONLY_INSTRUCTION + "\n" + systemPrompt;
    }

    private String koreanOnlyUserContent(String content) {
        if (!StringUtils.hasText(content) || content.startsWith(KOREAN_ONLY_USER_PREFIX)) {
            return content;
        }
        return KOREAN_ONLY_USER_PREFIX + content;
    }

    private String toOpenRouterRole(String role) {
        if ("user".equals(role) || "assistant".equals(role)) {
            return role;
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (!(response.get("choices") instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object firstChoice = choices.getFirst();
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return "";
        }
        Object message = choiceMap.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return "";
        }
        Object content = ((Map<String, Object>) messageMap).get("content");
        if (content == null) {
            return "";
        }
        String text = String.valueOf(content).trim();
        return "null".equalsIgnoreCase(text) ? "" : text;
    }

    private void logRequestBody(Map<String, Object> requestBody) {
        try {
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            // The API key is sent only in the Authorization header and is intentionally excluded from this log.
            log.info("OpenRouter request body: {}", requestBodyJson);
        } catch (Exception exception) {
            log.warn("Failed to serialize OpenRouter request body for debug logging.", exception);
        }
    }

    private boolean shouldRetry(int statusCode) {
        return statusCode == 404 || statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503;
    }

    private List<String> candidateModels() {
        List<String> models = new ArrayList<>();
        addFreeModel(models, PRIMARY_KOREAN_FREE_MODEL);
        addFreeModel(models, openRouterProperties.getModel());

        if (StringUtils.hasText(openRouterProperties.getFallbackModels())) {
            for (String model : openRouterProperties.getFallbackModels().split(",")) {
                addFreeModel(models, model);
            }
        }

        return models.isEmpty()
                ? List.of(PRIMARY_KOREAN_FREE_MODEL)
                : List.copyOf(models);
    }

    private void addFreeModel(List<String> models, String model) {
        if (!StringUtils.hasText(model)) {
            return;
        }
        String trimmed = model.trim();
        if (!trimmed.endsWith(":free")) {
            log.warn("Skipping non-free OpenRouter model to avoid accidental billing. model={}", trimmed);
            return;
        }
        if (!models.contains(trimmed)) {
            models.add(trimmed);
        }
    }

    private void sleepBeforeRetry(String endpoint, int attempt) {
        long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenRouterApiException(endpoint, HttpStatus.BAD_GATEWAY.value(), "Interrupted while retrying OpenRouter API.", exception);
        }
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private void logCall(String endpoint, long responseTimeMs, boolean success, String status, String model) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("userId", currentUserId())) {
            log.info(
                    "openrouter_api_call endpoint={} model={} responseTimeMs={} success={} openrouterStatus={}",
                    endpoint,
                    model,
                    responseTimeMs,
                    success,
                    status
            );
        }
    }

    private String currentUserId() {
        try {
            AuthenticatedUserService authenticatedUserService = authenticatedUserServiceProvider.getIfAvailable();
            if (authenticatedUserService == null) {
                return "anonymous";
            }
            return authenticatedUserService.getCurrentUserOptional()
                    .map(user -> String.valueOf(user.getId()))
                    .orElse("anonymous");
        } catch (RuntimeException exception) {
            return "anonymous";
        }
    }
}
