package com.animalplatform.external.publicapi;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PublicAnimalApiClient {

    private final RestClient restClient;
    private final PublicApiProperties publicApiProperties;

    public PublicAnimalApiClient(RestClient restClient, PublicApiProperties publicApiProperties) {
        this.restClient = restClient;
        this.publicApiProperties = publicApiProperties;
    }

    public PublicApiPage fetchAbandonedAnimalPage(int page, int size) {
        Map<String, Object> payload = get(
                publicApiProperties.getAbandonmentBaseUrl(),
                "/abandonmentPublic_v2",
                Map.of(
                        "pageNo", page,
                        "numOfRows", size,
                        "_type", "json"
                )
        );

        return extractPage(payload, page, size);
    }

    public List<Map<String, Object>> fetchAbandonedAnimals(int page, int size) {
        return fetchAbandonedAnimalPage(page, size).items();
    }

    public Optional<Map<String, Object>> fetchByDesertionNo(String desertionNo) {
        Map<String, Object> payload = get(
                publicApiProperties.getAbandonmentBaseUrl(),
                "/abandonmentPublic_v2",
                Map.of(
                        "pageNo", 1,
                        "numOfRows", 1,
                        "_type", "json",
                        "desertionNo", desertionNo
                )
        );

        return extractItems(payload).stream().findFirst();
    }

    public List<Map<String, Object>> fetchAbandonedAnimalsSnapshot(int maxPages, int size) {
        List<Map<String, Object>> snapshot = new ArrayList<>();

        for (int page = 1; page <= maxPages; page++) {
            PublicApiPage result = fetchAbandonedAnimalPage(page, size);
            snapshot.addAll(result.items());

            if (snapshot.size() >= result.totalCount() || result.items().isEmpty()) {
                break;
            }
        }

        return List.copyOf(snapshot);
    }

    public List<Map<String, Object>> fetchShelters(int page, int size) {
        Map<String, Object> payload = get(
                publicApiProperties.getShelterBaseUrl(),
                "/shelterInfo_v2",
                Map.of(
                        "pageNo", page,
                        "numOfRows", size,
                        "_type", "json"
                )
        );

        return extractItems(payload);
    }

    public List<Map<String, Object>> fetchSheltersByRegionName(String regionName, int page, int size) {
        Map<String, Object> payload = get(
                publicApiProperties.getShelterBaseUrl(),
                "/shelterInfo_v2",
                Map.of(
                        "pageNo", page,
                        "numOfRows", size,
                        "_type", "json",
                        "regionName", regionName
                )
        );

        return extractItems(payload);
    }

    public List<Map<String, Object>> fetchRegionStats() {
        Map<String, Object> payload = get(
                publicApiProperties.getStatsBaseUrl(),
                "/rescueAnimalSido",
                Map.of("_type", "json")
        );

        return extractItems(payload);
    }

    public Map<String, Object> getSyncMetadata() {
        return Map.of(
                "provider", publicApiProperties.getProvider(),
                "status", StringUtils.hasText(publicApiProperties.getServiceKey()) ? "CONNECTED" : "NOT_CONNECTED",
                "message", StringUtils.hasText(publicApiProperties.getServiceKey())
                        ? "공공데이터 인증키가 설정되어 있습니다."
                        : "공공데이터 인증키가 비어 있습니다."
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String baseUrl, String path, Map<String, Object> queryParams) {
        URI uri = buildUri(baseUrl, path, queryParams);

        Map<String, Object> response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(Map.class);

        return response == null ? Map.of() : response;
    }

    URI buildUri(String baseUrl, String path, Map<String, Object> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path)
                .queryParam("serviceKey", publicApiProperties.getServiceKey());

        queryParams.forEach(builder::queryParam);

        return builder
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    private PublicApiPage extractPage(Map<String, Object> payload, int page, int size) {
        Map<String, Object> body = extractBody(payload);
        List<Map<String, Object>> items = extractItemsFromBody(body);
        int totalCount = parseInt(body.get("totalCount"), items.size());
        return new PublicApiPage(items, page, size, totalCount);
    }

    private List<Map<String, Object>> extractItems(Map<String, Object> payload) {
        return extractItemsFromBody(extractBody(payload));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractBody(Map<String, Object> payload) {
        Object responseObject = payload.get("response");
        if (!(responseObject instanceof Map<?, ?> responseMap)) {
            return Map.of();
        }

        Object bodyObject = responseMap.get("body");
        if (!(bodyObject instanceof Map<?, ?> bodyMap)) {
            return Map.of();
        }

        return new LinkedHashMap<>((Map<String, Object>) bodyMap);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItemsFromBody(Map<String, Object> bodyMap) {
        Object itemsObject = bodyMap.get("items");
        if (!(itemsObject instanceof Map<?, ?> itemsMap)) {
            return List.of();
        }

        Object itemObject = itemsMap.get("item");
        if (itemObject instanceof List<?> list) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (Object candidate : list) {
                if (candidate instanceof Map<?, ?> candidateMap) {
                    items.add(new LinkedHashMap<>((Map<String, Object>) candidateMap));
                }
            }
            return items;
        }

        if (itemObject instanceof Map<?, ?> itemMap) {
            return List.of(new LinkedHashMap<>((Map<String, Object>) itemMap));
        }

        return Collections.emptyList();
    }

    private int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public record PublicApiPage(List<Map<String, Object>> items, int page, int size, int totalCount) {
    }
}
