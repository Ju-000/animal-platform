package com.animalplatform.shelter.application;

import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.SubscriptionPaymentHistoryRepository;
import com.animalplatform.shelter.domain.ShelterDonationGoal;
import com.animalplatform.shelter.domain.ShelterDonationGoalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShelterDonationGoalService {

    private final ShelterDonationGoalRepository goalRepository;
    private final DonationRepository donationRepository;
    private final SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository;

    public ShelterDonationGoalService(
            ShelterDonationGoalRepository goalRepository,
            DonationRepository donationRepository,
            SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository
    ) {
        this.goalRepository = goalRepository;
        this.donationRepository = donationRepository;
        this.subscriptionPaymentHistoryRepository = subscriptionPaymentHistoryRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGoal(String careRegNo) {
        YearMonth currentMonth = YearMonth.now();
        return goalRepository.findByShelterCareRegNoAndGoalMonth(careRegNo, currentMonth)
                .map(goal -> toResponse(goal, calculateCurrentMonthAmount(careRegNo), true))
                .orElseGet(() -> goalNotSetResponse(careRegNo, currentMonth));
    }

    @Transactional
    public Map<String, Object> setGoal(String careRegNo, long monthlyGoalAmount) {
        YearMonth currentMonth = YearMonth.now();
        ShelterDonationGoal goal = goalRepository.findByShelterCareRegNoAndGoalMonth(careRegNo, currentMonth)
                .orElseGet(() -> new ShelterDonationGoal(careRegNo, monthlyGoalAmount, currentMonth));
        goal.updateGoalAmount(monthlyGoalAmount);
        goal.updateCurrentMonthAmount(calculateCurrentMonthAmount(careRegNo));
        ShelterDonationGoal saved = goalRepository.save(goal);
        return toResponse(saved, saved.getCurrentMonthAmount(), true);
    }

    public Map<String, Object> getGoalForSummary(String careRegNo) {
        return getGoal(careRegNo);
    }

    private Long calculateCurrentMonthAmount(String careRegNo) {
        Long shelterId = shelterIdFromCode(careRegNo);
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.plusMonths(1).atDay(1);

        BigDecimal oneTime = donationRepository.sumPaidAmountByShelterIdAndPaidAtBetween(
                shelterId,
                startDate.atStartOfDay(),
                endDate.atStartOfDay()
        );
        BigDecimal recurring = subscriptionPaymentHistoryRepository.sumPaidAmountByShelterIdAndPaidAtBetween(
                shelterId,
                startDate.atStartOfDay(),
                endDate.atStartOfDay()
        );

        return oneTime.add(recurring).setScale(0, RoundingMode.DOWN).longValue();
    }

    private Map<String, Object> toResponse(ShelterDonationGoal goal, Long currentAmount, boolean goalSet) {
        long goalAmount = goal.getMonthlyGoalAmount() == null ? 0L : goal.getMonthlyGoalAmount();
        long safeCurrentAmount = currentAmount == null ? 0L : currentAmount;
        int percentage = goalAmount <= 0 ? 0 : (int) Math.min(1000, Math.round(safeCurrentAmount * 100.0 / goalAmount));

        return Map.of(
                "goalSet", goalSet,
                "shelterCareRegNo", goal.getShelterCareRegNo(),
                "goalMonth", goal.getGoalMonth().toString(),
                "goalAmount", goalAmount,
                "currentAmount", safeCurrentAmount,
                "percentage", percentage,
                "daysLeft", daysLeftInMonth()
        );
    }

    private Map<String, Object> goalNotSetResponse(String careRegNo, YearMonth goalMonth) {
        return Map.of(
                "goalSet", false,
                "shelterCareRegNo", careRegNo,
                "goalMonth", goalMonth.toString(),
                "goalAmount", 0L,
                "currentAmount", calculateCurrentMonthAmount(careRegNo),
                "percentage", 0,
                "daysLeft", daysLeftInMonth()
        );
    }

    private int daysLeftInMonth() {
        LocalDate today = LocalDate.now();
        return Math.max(0, today.lengthOfMonth() - today.getDayOfMonth());
    }

    private long shelterIdFromCode(String shelterCode) {
        return Math.abs((long) String.valueOf(shelterCode).hashCode());
    }
}
