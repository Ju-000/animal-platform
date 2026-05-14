package com.animalplatform.admin.presentation;

import com.animalplatform.adoption.domain.AdoptionApplication;
import com.animalplatform.adoption.domain.AdoptionApplicationRepository;
import com.animalplatform.adoption.domain.AdoptionApplicationStatus;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.common.api.CommonApiResponses;
import com.animalplatform.donation.domain.Donation;
import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.DonationStatus;
import com.animalplatform.donation.domain.SubscriptionPaymentHistory;
import com.animalplatform.donation.domain.SubscriptionPaymentHistoryRepository;
import com.animalplatform.user.application.UserService;
import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import com.animalplatform.user.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "관리자", description = "관리자 운영 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {

    private final AdoptionApplicationRepository adoptionApplicationRepository;
    private final DonationRepository donationRepository;
    private final SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public AdminManagementController(
            AdoptionApplicationRepository adoptionApplicationRepository,
            DonationRepository donationRepository,
            SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository,
            UserRepository userRepository,
            UserService userService
    ) {
        this.adoptionApplicationRepository = adoptionApplicationRepository;
        this.donationRepository = donationRepository;
        this.subscriptionPaymentHistoryRepository = subscriptionPaymentHistoryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/adoptions")
    @Operation(summary = "입양 신청 목록", description = "관리자가 입양 신청 목록을 상태별로 조회합니다.")
    public ApiResponse<Map<String, Object>> getAdoptions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size));
        Page<AdoptionApplication> result = parseAdoptionStatus(status) == null
                ? adoptionApplicationRepository.findAllByOrderByAppliedAtDesc(pageable)
                : adoptionApplicationRepository.findByStatusOrderByAppliedAtDesc(parseAdoptionStatus(status), pageable);

        return ApiResponse.ok(toPagePayload(result.map(this::toAdoptionItem)));
    }

    @PatchMapping("/adoptions/{id}/status")
    @Transactional
    @Operation(summary = "입양 신청 상태 변경", description = "관리자가 입양 신청을 승인하거나 거절합니다.")
    public ApiResponse<Map<String, Object>> updateAdoptionStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request
    ) {
        AdoptionApplication application = adoptionApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "입양 신청을 찾을 수 없습니다."));
        application.changeStatus(parseRequiredAdoptionStatus(request.status()));
        return ApiResponse.ok(toAdoptionItem(application), "입양 신청 상태가 변경되었습니다.");
    }

    @GetMapping("/donations")
    @Operation(summary = "후원/결제 목록", description = "관리자가 후원 및 결제 내역을 조회합니다.")
    public ApiResponse<Map<String, Object>> getDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size));
        Page<Donation> oneTimePage = donationRepository.findAdminPage(pageable);
        Page<SubscriptionPaymentHistory> recurringPage = subscriptionPaymentHistoryRepository.findAdminPage(pageable);

        List<Map<String, Object>> content = java.util.stream.Stream.concat(
                        oneTimePage.getContent().stream().map(this::toDonationItem),
                        recurringPage.getContent().stream().map(this::toRecurringDonationItem)
                )
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("paidAt")), Comparator.reverseOrder()))
                .limit(safeSize(size))
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", content);
        payload.put("page", safePage(page));
        payload.put("size", safeSize(size));
        payload.put("totalElements", oneTimePage.getTotalElements() + recurringPage.getTotalElements());
        payload.put("totalPages", Math.max(oneTimePage.getTotalPages(), recurringPage.getTotalPages()));
        payload.put("stats", donationStats());
        return ApiResponse.ok(payload);
    }

    @GetMapping("/users")
    @Operation(summary = "회원 목록", description = "관리자가 회원 목록과 후원 등급 정보를 조회합니다.")
    public ApiResponse<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<User> users = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage(page), safeSize(size)));
        return ApiResponse.ok(toPagePayload(users.map(this::toUserItem)));
    }

    @PatchMapping("/users/{id}/role")
    @Transactional
    @Operation(summary = "회원 역할 변경", description = "관리자가 회원의 역할을 변경합니다.")
    public ApiResponse<Map<String, Object>> updateUserRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        user.changeRole(parseRole(request.role()));
        return ApiResponse.ok(toUserItem(user), "회원 역할이 변경되었습니다.");
    }

    private Map<String, Object> toAdoptionItem(AdoptionApplication application) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", application.getId());
        item.put("applicantName", application.getApplicantName());
        item.put("applicantPhone", application.getApplicantPhone());
        item.put("applicantEmail", application.getApplicantEmail());
        item.put("animalNo", application.getAnimalNo());
        item.put("appliedAt", application.getAppliedAt() == null ? "" : application.getAppliedAt().toString());
        item.put("status", application.getStatus().name());
        item.put("reason", application.getReason());
        return item;
    }

    private Map<String, Object> toDonationItem(Donation donation) {
        Map<String, Object> item = baseDonationItem(
                donation.getUser(),
                donation.getShelter() == null ? "" : donation.getShelter().getName(),
                donation.getAmount(),
                "일시",
                donation.getPaymentStatus(),
                donation.getPaidAt()
        );
        item.put("id", "one-" + donation.getId());
        return item;
    }

    private Map<String, Object> toRecurringDonationItem(SubscriptionPaymentHistory history) {
        var subscription = history.getSubscriptionDonation();
        Map<String, Object> item = baseDonationItem(
                subscription.getUser(),
                subscription.getShelter() == null ? "" : subscription.getShelter().getName(),
                history.getAmount(),
                "정기",
                history.getPaymentStatus(),
                history.getPaidAt()
        );
        item.put("id", "recurring-" + history.getId());
        return item;
    }

    private Map<String, Object> baseDonationItem(
            User user,
            String shelterName,
            BigDecimal amount,
            String type,
            DonationStatus status,
            LocalDateTime paidAt
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("donorName", user == null ? "" : user.getName());
        item.put("donorEmail", user == null ? "" : user.getEmail());
        item.put("shelterName", shelterName == null || shelterName.isBlank() ? "보호소 미지정" : shelterName);
        item.put("amount", amount == null ? 0 : amount.setScale(0, RoundingMode.DOWN).longValue());
        item.put("type", type);
        item.put("status", status == null ? "" : status.name());
        item.put("paidAt", paidAt == null ? "" : paidAt.toString());
        return item;
    }

    private Map<String, Object> toUserItem(User user) {
        Map<String, Object> tier = userService.buildTierPayload(user);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", user.getId());
        item.put("name", user.getName());
        item.put("email", user.getEmail());
        item.put("username", user.getUsername());
        item.put("createdAt", user.getCreatedAt() == null ? "" : user.getCreatedAt().toString());
        item.put("tierLabel", tier.get("tierLabel"));
        item.put("tierEmoji", tier.get("tierEmoji"));
        item.put("totalDonatedAmount", tier.get("totalDonatedAmount"));
        item.put("role", user.getRole().name());
        return item;
    }

    private Map<String, Object> donationStats() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        BigDecimal todayTotal = donationRepository.sumPaidAmountByPaidAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay())
                .add(subscriptionPaymentHistoryRepository.sumPaidAmountByPaidAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        BigDecimal monthTotal = donationRepository.sumPaidAmountByPaidAtBetween(monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay())
                .add(subscriptionPaymentHistoryRepository.sumPaidAmountByPaidAtBetween(monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayDonationAmount", todayTotal.setScale(0, RoundingMode.DOWN).longValue());
        stats.put("monthDonationAmount", monthTotal.setScale(0, RoundingMode.DOWN).longValue());
        stats.put("totalDonorCount", userRepository.count());
        return stats;
    }

    private Map<String, Object> toPagePayload(Page<Map<String, Object>> page) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", page.getContent());
        payload.put("page", page.getNumber());
        payload.put("size", page.getSize());
        payload.put("totalElements", page.getTotalElements());
        payload.put("totalPages", page.getTotalPages());
        payload.put("hasNext", page.hasNext());
        return payload;
    }

    private AdoptionApplicationStatus parseAdoptionStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return parseRequiredAdoptionStatus(status);
    }

    private AdoptionApplicationStatus parseRequiredAdoptionStatus(String status) {
        try {
            return AdoptionApplicationStatus.valueOf(String.valueOf(status).trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 입양 신청 상태입니다.");
        }
    }

    private UserRole parseRole(String role) {
        String normalized = String.valueOf(role).trim().toUpperCase(Locale.ROOT);
        if ("USER".equals(normalized)) {
            return UserRole.MEMBER;
        }
        try {
            return UserRole.valueOf(normalized);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 회원 역할입니다.");
        }
    }

    private int safePage(int page) {
        return Math.max(0, page);
    }

    private int safeSize(int size) {
        return Math.min(100, Math.max(1, size));
    }

    public record StatusUpdateRequest(String status) {
    }

    public record RoleUpdateRequest(String role) {
    }
}
