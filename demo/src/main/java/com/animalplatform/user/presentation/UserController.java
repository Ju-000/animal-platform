package com.animalplatform.user.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.auth.application.AuthenticatedUserService;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.donation.domain.Donation;
import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.SubscriptionDonation;
import com.animalplatform.donation.domain.SubscriptionPaymentHistory;
import com.animalplatform.donation.domain.SubscriptionPaymentHistoryRepository;
import com.animalplatform.user.application.UserService;
import com.animalplatform.user.domain.User;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "인증", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserService userService;
    private final DonationRepository donationRepository;
    private final SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository;

    public UserController(
            AuthenticatedUserService authenticatedUserService,
            UserService userService,
            DonationRepository donationRepository,
            SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.userService = userService;
        this.donationRepository = donationRepository;
        this.subscriptionPaymentHistoryRepository = subscriptionPaymentHistoryRepository;
    }

    @GetMapping("/me")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getMyProfile() {
        User user = authenticatedUserService.getCurrentUser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.getId());
        payload.put("username", user.getUsername());
        payload.put("email", user.getEmail());
        payload.put("name", user.getName());
        payload.put("phone", user.getPhone());
        payload.put("birthDate", user.getBirthDate());
        payload.put("gender", user.getGender());
        payload.put("address", user.getAddress());
        payload.put("role", user.getRole().name());
        LocalDateTime firstDonationAt = donationRepository.findFirstPaidAtByUserId(user.getId());
        payload.put("firstDonationAt", firstDonationAt == null ? null : firstDonationAt.toString());
        payload.put("notifications", Map.of(
                "urgentAnimalAlert", user.isUrgentAnimalAlert(),
                "monthlyNewsletter", user.isMonthlyNewsletter(),
                "weeklyNewsletter", user.isWeeklyNewsletter()
        ));
        payload.putAll(userService.buildTierPayload(user));

        return ApiResponse.ok(payload);
    }

    @PatchMapping("/me/notifications")
    @Operation(summary = "내 알림 설정 변경", description = "긴급 구조 알림, 월간 소식, 주간 뉴스레터 수신 여부를 변경합니다.")
    @Transactional
    public ApiResponse<Map<String, Object>> updateMyNotificationSettings(@RequestBody NotificationSettingsRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        user.updateNotificationSettings(
                request.urgentAnimalAlert(),
                request.monthlyNewsletter(),
                request.weeklyNewsletter()
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("urgentAnimalAlert", user.isUrgentAnimalAlert());
        payload.put("monthlyNewsletter", user.isMonthlyNewsletter());
        payload.put("weeklyNewsletter", user.isWeeklyNewsletter());
        return ApiResponse.ok(payload);
    }

    @GetMapping("/me/donation-history")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getMyDonationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = authenticatedUserService.getCurrentUser();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        Pageable lookupLimit = PageRequest.of(0, (safePage + 1) * safeSize);

        List<DonationHistoryItem> oneTimeItems = donationRepository.findPaidHistoryByUserId(user.getId(), lookupLimit)
                .stream()
                .map(this::toOneTimeHistory)
                .toList();
        List<DonationHistoryItem> recurringItems = subscriptionPaymentHistoryRepository.findPaidHistoryByUserId(user.getId(), lookupLimit)
                .stream()
                .map(this::toRecurringHistory)
                .toList();

        List<DonationHistoryItem> combined = java.util.stream.Stream.concat(oneTimeItems.stream(), recurringItems.stream())
                .sorted(Comparator.comparing(DonationHistoryItem::paidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int fromIndex = Math.min(safePage * safeSize, combined.size());
        int toIndex = Math.min(fromIndex + safeSize, combined.size());
        List<Map<String, Object>> content = combined.subList(fromIndex, toIndex)
                .stream()
                .map(DonationHistoryItem::toMap)
                .toList();

        return ApiResponse.ok(Map.of(
                "content", content,
                "page", safePage,
                "size", safeSize,
                "hasNext", combined.size() > toIndex
        ));
    }

    private DonationHistoryItem toOneTimeHistory(Donation donation) {
        String shelterName = donation.getShelter() == null ? "" : donation.getShelter().getName();
        String animalNo = donation.getAnimal() == null ? null : donation.getAnimal().getPublicApiId();
        return new DonationHistoryItem(
                shelterName,
                animalNo,
                donation.getAmount().longValue(),
                "일시",
                donation.getPaidAt()
        );
    }

    private DonationHistoryItem toRecurringHistory(SubscriptionPaymentHistory history) {
        SubscriptionDonation subscription = history.getSubscriptionDonation();
        String shelterName = subscription.getShelter() == null ? "" : subscription.getShelter().getName();
        String animalNo = subscription.getAnimal() == null ? null : subscription.getAnimal().getPublicApiId();
        return new DonationHistoryItem(
                shelterName,
                animalNo,
                history.getAmount().longValue(),
                "정기",
                history.getPaidAt()
        );
    }

    private record DonationHistoryItem(
            String shelterName,
            String animalNo,
            Long amount,
            String type,
            LocalDateTime paidAt
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("shelterName", shelterName);
            item.put("animalNo", animalNo);
            item.put("amount", amount);
            item.put("type", type);
            item.put("paidAt", paidAt == null ? "" : paidAt.toString());
            return item;
        }
    }
}
