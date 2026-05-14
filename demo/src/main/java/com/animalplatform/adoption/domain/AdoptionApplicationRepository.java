package com.animalplatform.adoption.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdoptionApplicationRepository extends JpaRepository<AdoptionApplication, Long> {

    List<AdoptionApplication> findByUserIdOrderByAppliedAtDesc(Long userId);

    Page<AdoptionApplication> findAllByOrderByAppliedAtDesc(Pageable pageable);

    Page<AdoptionApplication> findByStatusOrderByAppliedAtDesc(AdoptionApplicationStatus status, Pageable pageable);

    @Query("""
            select count(application)
            from AdoptionApplication application
            where application.status = com.animalplatform.adoption.domain.AdoptionApplicationStatus.PENDING
            """)
    long countPending();
}
