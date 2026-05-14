package com.animalplatform.donation.presentation;

import com.animalplatform.campaign.domain.DonationCampaignRepository;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.common.api.CommonApiResponses;
import com.animalplatform.statistics.domain.SnapshotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "후원/결제", description = "후원 및 결제 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final SnapshotRepository snapshotRepository;
    private final DonationCampaignRepository campaignRepository;

    public DonationController(
            SnapshotRepository snapshotRepository,
            DonationCampaignRepository campaignRepository
    ) {
        this.snapshotRepository = snapshotRepository;
        this.campaignRepository = campaignRepository;
    }

    @GetMapping("/options")
    @Operation(summary = "후원 옵션 조회", description = "기본 후원 금액과 후원 유형을 조회합니다.")
    public ApiResponse<Map<String, Object>> getDonationOptions() {
        return ApiResponse.ok(Map.of(
                "donationTypes", List.of(
                        Map.of("id", "ONE_TIME", "label", "일시 후원"),
                        Map.of("id", "SUBSCRIPTION", "label", "정기 후원")
                ),
                "subscriptionIntervals", List.of(
                        Map.of("id", "MONTHLY", "label", "매월"),
                        Map.of("id", "QUARTERLY", "label", "분기별")
                ),
                "amountOptions", List.of(5000, 10000, 20000, 30000, 50000)
        ));
    }

    @GetMapping("/usage")
    @Operation(summary = "후원 사용 내역 조회", description = "후원 사용 비중 안내용 데이터를 조회합니다.")
    public ApiResponse<List<Map<String, Object>>> getDonationUsage() {
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/stats")
    @Operation(summary = "후원 통계 조회", description = "후원 페이지의 효과 통계를 DB 기준으로 집계합니다.")
    public ApiResponse<Map<String, Object>> getDonationStats() {
        long medicalCount = snapshotRepository.countMedicalSupportCandidates();
        long foodAmount = campaignRepository.sumActiveCurrentAmountByCategoryKeyword("사료");
        long shelterCount = snapshotRepository.countDistinctSheltersByCareName();
        long adoptedCount = snapshotRepository.countByProcessStateContaining("입양");

        return ApiResponse.ok(Map.of(
                "medicalCount", medicalCount,
                "foodKg", foodAmount / 3000,
                "shelterCount", shelterCount,
                "adoptedCount", adoptedCount
        ));
    }

    @PostMapping
    @Operation(summary = "후원 생성", description = "후원 결제 전 준비 상태의 후원 요청을 생성합니다.")
    public ApiResponse<Map<String, Object>> createDonation(@Valid @RequestBody CreateDonationRequest request) {
        return ApiResponse.ok(Map.of(
                "targetType", request.targetType(),
                "targetId", request.targetId(),
                "amount", request.amount(),
                "paymentStatus", "READY"
        ));
    }

    public record CreateDonationRequest(
            @NotBlank String targetType,
            @NotNull Long targetId,
            @NotNull @DecimalMin("1000") BigDecimal amount
    ) {
    }
}
