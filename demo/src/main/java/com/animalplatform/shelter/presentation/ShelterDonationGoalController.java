package com.animalplatform.shelter.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.shelter.application.ShelterDonationGoalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "보호소", description = "해당 도메인 API")
@CommonApiResponses
@RestController
public class ShelterDonationGoalController {

    private final ShelterDonationGoalService shelterDonationGoalService;

    public ShelterDonationGoalController(ShelterDonationGoalService shelterDonationGoalService) {
        this.shelterDonationGoalService = shelterDonationGoalService;
    }

    @GetMapping("/api/shelters/{careRegNo}/donation-goal")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getGoal(@PathVariable String careRegNo) {
        return ApiResponse.ok(shelterDonationGoalService.getGoal(careRegNo));
    }

    @PostMapping("/api/admin/shelters/{careRegNo}/donation-goal")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> setGoal(
            @PathVariable String careRegNo,
            @Valid @RequestBody SetGoalRequest request
    ) {
        return ApiResponse.ok(shelterDonationGoalService.setGoal(careRegNo, request.monthlyGoalAmount()));
    }

    public record SetGoalRequest(@Min(1) long monthlyGoalAmount) {
    }
}
