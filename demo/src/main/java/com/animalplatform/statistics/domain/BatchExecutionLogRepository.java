package com.animalplatform.statistics.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchExecutionLogRepository extends JpaRepository<BatchExecutionLog, Long> {

    Optional<BatchExecutionLog> findFirstByStatusAndStartedAtAfterOrderByStartedAtDesc(
            BatchExecutionStatus status,
            LocalDateTime startedAfter
    );

    List<BatchExecutionLog> findAllByOrderByStartedAtDesc(Pageable pageable);

    List<BatchExecutionLog> findTop3ByOrderByStartedAtDesc();
}
