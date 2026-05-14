package com.animalplatform.admin.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.external.publicapi.PublicAnimalApiClient;
import com.animalplatform.statistics.application.BatchCollectorService;
import com.animalplatform.statistics.application.BatchCollectorService.BatchRunResult;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "관리자", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final PublicAnimalApiClient publicAnimalApiClient;
    private final BatchCollectorService batchCollectorService;

    public AdminDashboardController(PublicAnimalApiClient publicAnimalApiClient, BatchCollectorService batchCollectorService) {
        this.publicAnimalApiClient = publicAnimalApiClient;
        this.batchCollectorService = batchCollectorService;
    }

    @GetMapping("/dashboard")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getDashboard() {
        List<Map<String, Object>> animals = publicAnimalApiClient.fetchAbandonedAnimalsSnapshot(10, 100);
        List<Map<String, Object>> shelters = publicAnimalApiClient.fetchShelters(1, 100);
        Map<String, Object> syncMetadata = publicAnimalApiClient.getSyncMetadata();

        long protectedCount = animals.stream().filter(this::isProtectedState).count();
        long counselingCount = animals.stream().filter(this::isCounselingState).count();
        long completedCount = animals.stream().filter(this::isCompletedState).count();
        long medicalNeedCount = animals.stream().filter(this::hasMedicalNeed).count();

        List<Map<String, Object>> topRegions = animals.stream()
                .collect(Collectors.groupingBy(item -> normalizeRegion(firstText(item, "orgNm", "careAddr")), LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().isBlank())
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> region = new LinkedHashMap<>();
                    region.put("label", entry.getKey());
                    region.put("value", entry.getValue());
                    return region;
                })
                .toList();

        List<Map<String, Object>> alerts = List.of(
                createAlert("의료 지원 필요 동물", medicalNeedCount, "부상, 치료, 수술 관련 키워드가 있는 동물 수"),
                createAlert("상담 진행 중", counselingCount, "입양 상담 또는 종료 단계로 이동한 동물 수"),
                createAlert("연결된 보호소", shelters.size(), "현재 보호소 목록 API에서 수집한 보호소 수")
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", syncMetadata.get("provider"));
        response.put("connectionStatus", syncMetadata.get("status"));
        response.put("connectionMessage", syncMetadata.get("message"));
        response.put("checkedAt", LocalDateTime.now().toString());
        response.put("snapshotPages", 10);
        response.put("pageSize", 100);
        response.put("snapshotCount", animals.size());
        response.put("shelterCount", shelters.size());
        response.put("protectedCount", protectedCount);
        response.put("counselingCount", counselingCount);
        response.put("completedCount", completedCount);
        response.put("medicalNeedCount", medicalNeedCount);
        response.put("topRegions", topRegions);
        response.put("alerts", alerts);
        return ApiResponse.ok(response);
    }

    @PostMapping("/batch/run")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<BatchRunResult> runBatch() {
        return ApiResponse.ok(batchCollectorService.collectDailySnapshots(), "Batch collection completed.");
    }

    private Map<String, Object> createAlert(String title, long value, String description) {
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("title", title);
        alert.put("value", value);
        alert.put("description", description);
        return alert;
    }

    private boolean isProtectedState(Map<String, Object> item) {
        String state = firstText(item, "processState");
        return state.contains("보호") || state.contains("치료") || state.contains("공고");
    }

    private boolean isCounselingState(Map<String, Object> item) {
        String state = firstText(item, "processState");
        return state.contains("상담") || state.contains("종료");
    }

    private boolean isCompletedState(Map<String, Object> item) {
        String state = firstText(item, "processState");
        return state.contains("입양") || state.contains("분양") || state.contains("종료");
    }

    private boolean hasMedicalNeed(Map<String, Object> item) {
        String notice = firstText(item, "specialMark");
        return notice.contains("부상") || notice.contains("치료") || notice.contains("수술") || notice.contains("골절") || notice.contains("상처");
    }

    private String normalizeRegion(String value) {
        if (value == null || value.isBlank()) {
            return "기타";
        }
        String[] regions = {
                "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
                "세종특별자치시", "경기도", "강원특별자치도", "충청북도", "충청남도", "전북특별자치도",
                "전라남도", "경상북도", "경상남도", "제주특별자치도"
        };
        for (String region : regions) {
            if (value.contains(region)) {
                return region;
            }
        }
        return value;
    }

    private String firstText(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                    return text;
                }
            }
        }
        return "";
    }
}
