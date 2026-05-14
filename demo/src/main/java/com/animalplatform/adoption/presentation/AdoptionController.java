package com.animalplatform.adoption.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.adoption.domain.AdoptionApplication;
import com.animalplatform.adoption.domain.AdoptionApplicationRepository;
import com.animalplatform.adoption.domain.HousingType;
import com.animalplatform.auth.application.AuthenticatedUserService;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
@Tag(name = "입양", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/adoptions")
public class AdoptionController {

    private final AdoptionApplicationRepository adoptionApplicationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public AdoptionController(
            AdoptionApplicationRepository adoptionApplicationRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.adoptionApplicationRepository = adoptionApplicationRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/checklist")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<List<Map<String, Object>>> getChecklist() {
        return ApiResponse.ok(List.of(
                Map.of("id", "housing", "label", "반려동물 양육이 가능한 주거 환경인지 확인했어요."),
                Map.of("id", "family", "label", "가족 또는 동거인의 동의를 받았어요."),
                Map.of("id", "cost", "label", "예방접종, 사료, 치료비 등 기본 비용을 이해했어요."),
                Map.of("id", "care", "label", "입양 후 돌봄 시간과 책임을 감당할 수 있어요.")
        ));
    }

    @PostMapping
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> createApplication(@Valid @RequestBody CreateAdoptionRequest request) {
        Long userId = authenticatedUserService.getCurrentUserOptional()
                .map(User::getId)
                .orElse(null);

        AdoptionApplication application = new AdoptionApplication(
                request.resolvedAnimalNo(),
                request.applicantName().trim(),
                request.applicantPhone().trim(),
                request.applicantEmail().trim(),
                request.address().trim(),
                parseHousingType(request.resolvedHousingType()),
                request.resolvedHasExperience(),
                request.resolvedReason().trim(),
                userId
        );

        AdoptionApplication saved = adoptionApplicationRepository.save(application);

        return ApiResponse.ok(Map.of(
                "id", saved.getId(),
                "animalNo", saved.getAnimalNo(),
                "status", saved.getStatus().name(),
                "appliedAt", saved.getAppliedAt().toString(),
                "message", "입양 신청이 접수되었습니다."
        ));
    }

    @GetMapping("/my")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<List<Map<String, Object>>> getMyApplications() {
        User user = authenticatedUserService.getCurrentUser();
        List<Map<String, Object>> applications = adoptionApplicationRepository
                .findByUserIdOrderByAppliedAtDesc(user.getId())
                .stream()
                .map(this::toApplicationItem)
                .toList();

        return ApiResponse.ok(applications);
    }

    private Map<String, Object> toApplicationItem(AdoptionApplication application) {
        return Map.ofEntries(
                Map.entry("id", application.getId()),
                Map.entry("animalNo", application.getAnimalNo()),
                Map.entry("applicantName", application.getApplicantName()),
                Map.entry("applicantPhone", application.getApplicantPhone()),
                Map.entry("applicantEmail", application.getApplicantEmail()),
                Map.entry("address", application.getAddress()),
                Map.entry("housingType", application.getHousingType().name()),
                Map.entry("hasExperience", application.isHasExperience()),
                Map.entry("reason", application.getReason()),
                Map.entry("status", application.getStatus().name()),
                Map.entry("appliedAt", application.getAppliedAt().toString())
        );
    }

    private HousingType parseHousingType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("아파트")) {
            return HousingType.APARTMENT;
        }
        if (normalized.equals("주택") || normalized.equals("단독주택") || normalized.equals("HOUSE")) {
            return HousingType.HOUSE;
        }
        if (normalized.equals("APARTMENT")) {
            return HousingType.APARTMENT;
        }
        if (normalized.equals("OTHER") || normalized.equals("기타")) {
            return HousingType.OTHER;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 주거 형태입니다.");
    }

    public record CreateAdoptionRequest(
            @Size(max = 120) String animalNo,
            Long animalId,
            @NotBlank @Size(max = 100) String applicantName,
            @NotBlank @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.") String applicantPhone,
            @NotBlank @Email @Size(max = 255) String applicantEmail,
            @NotBlank @Size(max = 255) String address,
            String housingType,
            String livingType,
            @NotNull Boolean hasExperience,
            @Size(max = 5000) String reason,
            @Size(max = 5000) String applicantMessage
    ) {

        @AssertTrue(message = "동물 식별자는 필수입니다.")
        public boolean isAnimalIdentifierPresent() {
            return hasText(animalNo) || animalId != null;
        }

        @AssertTrue(message = "주거 형태는 필수입니다.")
        public boolean isHousingTypePresent() {
            return hasText(housingType) || hasText(livingType);
        }

        @AssertTrue(message = "입양 신청 사유는 필수입니다.")
        public boolean isReasonPresent() {
            return hasText(reason) || hasText(applicantMessage);
        }

        private String resolvedAnimalNo() {
            return hasText(animalNo) ? animalNo.trim() : String.valueOf(animalId);
        }

        private String resolvedHousingType() {
            return hasText(housingType) ? housingType : livingType;
        }

        private String resolvedReason() {
            return hasText(reason) ? reason : applicantMessage;
        }

        private boolean resolvedHasExperience() {
            return Boolean.TRUE.equals(hasExperience);
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
