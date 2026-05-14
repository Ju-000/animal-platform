package com.animalplatform.shelter.domain;

import java.time.YearMonth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelterDonationGoalRepository extends JpaRepository<ShelterDonationGoal, Long> {

    Optional<ShelterDonationGoal> findByShelterCareRegNoAndGoalMonth(String shelterCareRegNo, YearMonth goalMonth);
}
