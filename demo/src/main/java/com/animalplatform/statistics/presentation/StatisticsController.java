package com.animalplatform.statistics.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.statistics.application.StatisticsService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "통계", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/stats")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/summary")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ApiResponse.ok(statisticsService.getSummary(startDate, endDate));
    }

    @GetMapping("/regions")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<List<Map<String, Object>>> getRegions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ApiResponse.ok(statisticsService.getRegions(startDate, endDate));
    }
}
