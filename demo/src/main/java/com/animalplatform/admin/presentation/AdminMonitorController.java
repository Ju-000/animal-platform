package com.animalplatform.admin.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.admin.application.ApiHealthService;
import com.animalplatform.adoption.domain.AdoptionApplicationRepository;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.SubscriptionPaymentHistoryRepository;
import com.animalplatform.shelter.domain.ShelterRepository;
import com.animalplatform.statistics.domain.BatchExecutionLog;
import com.animalplatform.statistics.domain.BatchExecutionLogRepository;
import com.animalplatform.statistics.domain.SnapshotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "관리자", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/admin")
public class AdminMonitorController {

    private final ApiHealthService apiHealthService;
    private final SnapshotRepository snapshotRepository;
    private final ShelterRepository shelterRepository;
    private final AdoptionApplicationRepository adoptionApplicationRepository;
    private final DonationRepository donationRepository;
    private final SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository;
    private final BatchExecutionLogRepository batchExecutionLogRepository;

    public AdminMonitorController(
            ApiHealthService apiHealthService,
            SnapshotRepository snapshotRepository,
            ShelterRepository shelterRepository,
            AdoptionApplicationRepository adoptionApplicationRepository,
            DonationRepository donationRepository,
            SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository,
            BatchExecutionLogRepository batchExecutionLogRepository
    ) {
        this.apiHealthService = apiHealthService;
        this.snapshotRepository = snapshotRepository;
        this.shelterRepository = shelterRepository;
        this.adoptionApplicationRepository = adoptionApplicationRepository;
        this.donationRepository = donationRepository;
        this.subscriptionPaymentHistoryRepository = subscriptionPaymentHistoryRepository;
        this.batchExecutionLogRepository = batchExecutionLogRepository;
    }

    @GetMapping("/monitor")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getMonitor() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("publicApi", apiHealthService.getPublicApiHealth());
        payload.put("lastBatch", apiHealthService.getLastBatchHealth());
        payload.put("batchHistory", batchHistory(10));
        payload.put("systemStats", systemStats());
        return ApiResponse.ok(payload);
    }

    @GetMapping("/batch/history")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<List<Map<String, Object>>> getBatchHistory(@RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return ApiResponse.ok(batchHistory(safeLimit));
    }

    private Map<String, Object> systemStats() {
        LocalDate today = LocalDate.now();
        BigDecimal oneTimeToday = donationRepository.sumPaidAmountByPaidAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        BigDecimal recurringToday = subscriptionPaymentHistoryRepository.sumPaidAmountByPaidAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAnimals", snapshotRepository.count());
        stats.put("totalShelters", shelterRepository.count());
        stats.put("pendingAdoptions", adoptionApplicationRepository.countPending());
        stats.put("todayDonationAmount", oneTimeToday.add(recurringToday).setScale(0, RoundingMode.DOWN).longValue());
        return stats;
    }

    private List<Map<String, Object>> batchHistory(int limit) {
        return batchExecutionLogRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toBatchExecutionLogResponse)
                .toList();
    }

    private Map<String, Object> toBatchExecutionLogResponse(BatchExecutionLog executionLog) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", executionLog.getId());
        payload.put("startedAt", executionLog.getStartedAt() == null ? "" : executionLog.getStartedAt().toString());
        payload.put("completedAt", executionLog.getCompletedAt() == null ? "" : executionLog.getCompletedAt().toString());
        payload.put("status", executionLog.getStatus().name());
        payload.put("totalFetched", executionLog.getTotalFetched());
        payload.put("totalUpserted", executionLog.getTotalUpserted());
        payload.put("totalErrors", executionLog.getTotalErrors());
        payload.put("errorSummary", executionLog.getErrorSummary());
        return payload;
    }
}
