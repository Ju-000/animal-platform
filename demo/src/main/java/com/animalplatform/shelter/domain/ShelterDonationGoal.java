package com.animalplatform.shelter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.YearMonth;

@Entity
@Table(
        name = "shelter_donation_goals",
        uniqueConstraints = @UniqueConstraint(name = "uq_shelter_donation_goal_month", columnNames = {"shelter_care_reg_no", "goal_month"})
)
public class ShelterDonationGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shelter_care_reg_no", nullable = false, length = 100)
    private String shelterCareRegNo;

    @Column(nullable = false)
    private Long monthlyGoalAmount;

    @Column(nullable = false)
    private Long currentMonthAmount = 0L;

    @Column(name = "goal_month", nullable = false, length = 7)
    private YearMonth goalMonth;

    protected ShelterDonationGoal() {
    }

    public ShelterDonationGoal(String shelterCareRegNo, Long monthlyGoalAmount, YearMonth goalMonth) {
        this.shelterCareRegNo = shelterCareRegNo;
        this.monthlyGoalAmount = monthlyGoalAmount;
        this.goalMonth = goalMonth;
    }

    public void updateGoalAmount(Long monthlyGoalAmount) {
        this.monthlyGoalAmount = monthlyGoalAmount;
    }

    public void updateCurrentMonthAmount(Long currentMonthAmount) {
        this.currentMonthAmount = currentMonthAmount == null ? 0L : currentMonthAmount;
    }

    public Long getId() {
        return id;
    }

    public String getShelterCareRegNo() {
        return shelterCareRegNo;
    }

    public Long getMonthlyGoalAmount() {
        return monthlyGoalAmount;
    }

    public Long getCurrentMonthAmount() {
        return currentMonthAmount;
    }

    public YearMonth getGoalMonth() {
        return goalMonth;
    }
}
