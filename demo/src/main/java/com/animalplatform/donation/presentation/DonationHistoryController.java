package com.animalplatform.donation.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.auth.application.AuthenticatedUserService;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.donation.domain.Donation;
import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.SubscriptionDonation;
import com.animalplatform.donation.domain.SubscriptionDonationRepository;
import com.animalplatform.user.domain.User;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "후원/결제", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/donations")
public class DonationHistoryController {

    private final AuthenticatedUserService authenticatedUserService;
    private final DonationRepository donationRepository;
    private final SubscriptionDonationRepository subscriptionDonationRepository;

    public DonationHistoryController(
            AuthenticatedUserService authenticatedUserService,
            DonationRepository donationRepository,
            SubscriptionDonationRepository subscriptionDonationRepository
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.donationRepository = donationRepository;
        this.subscriptionDonationRepository = subscriptionDonationRepository;
    }

    @GetMapping("/me")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getMyDonations() {
        User user = authenticatedUserService.getCurrentUser();

        List<Map<String, Object>> oneTimeDonations = donationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDonationItem)
                .toList();

        List<Map<String, Object>> subscriptions = subscriptionDonationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toSubscriptionItem)
                .toList();

        return ApiResponse.ok(Map.of(
                "oneTimeDonations", oneTimeDonations,
                "subscriptions", subscriptions
        ));
    }

    private Map<String, Object> toDonationItem(Donation donation) {
        String targetName = donation.getShelter() != null
                ? donation.getShelter().getName()
                : donation.getAnimal() != null ? donation.getAnimal().getName() : "알 수 없음";

        return Map.of(
                "id", donation.getId(),
                "targetType", donation.getTargetType().name(),
                "donationType", donation.getDonationType().name(),
                "targetName", targetName,
                "amount", donation.getAmount(),
                "status", donation.getPaymentStatus().name(),
                "paidAt", donation.getPaidAt() == null ? "" : donation.getPaidAt().toString()
        );
    }

    private Map<String, Object> toSubscriptionItem(SubscriptionDonation subscriptionDonation) {
        String targetName = subscriptionDonation.getShelter() != null
                ? subscriptionDonation.getShelter().getName()
                : subscriptionDonation.getAnimal() != null ? subscriptionDonation.getAnimal().getName() : "알 수 없음";

        return Map.of(
                "id", subscriptionDonation.getId(),
                "targetType", subscriptionDonation.getTargetType().name(),
                "targetName", targetName,
                "amount", subscriptionDonation.getAmount(),
                "status", subscriptionDonation.getStatus().name(),
                "interval", subscriptionDonation.getSubscriptionInterval().name(),
                "nextBillingAt", subscriptionDonation.getNextBillingAt() == null ? "" : subscriptionDonation.getNextBillingAt().toString()
        );
    }
}
