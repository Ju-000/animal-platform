package com.animalplatform.statistics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalStatusHistoryRepository extends JpaRepository<AnimalStatusHistory, Long> {
}
