package com.animalplatform.animal.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalAiSummaryRepository extends JpaRepository<AnimalAiSummary, Long> {

    Optional<AnimalAiSummary> findByDesertionNo(String desertionNo);

    void deleteAllByExpiresAtBefore(LocalDateTime now);
}
