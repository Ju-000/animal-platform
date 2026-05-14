package com.animalplatform.common.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "관리자", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "service", "animal-platform",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}
