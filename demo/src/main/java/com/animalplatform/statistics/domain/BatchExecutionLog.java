package com.animalplatform.statistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.LocalDateTime;

@Entity
public class BatchExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchExecutionStatus status;

    private int totalFetched;
    private int totalUpserted;
    private int totalErrors;

    @Lob
    private String errorSummary;

    protected BatchExecutionLog() {
    }

    public BatchExecutionLog(LocalDateTime startedAt) {
        this.startedAt = startedAt;
        this.status = BatchExecutionStatus.RUNNING;
    }

    public void complete(BatchExecutionStatus status, int totalFetched, int totalUpserted, int totalErrors, String errorSummary) {
        this.completedAt = LocalDateTime.now();
        this.status = status;
        this.totalFetched = Math.max(0, totalFetched);
        this.totalUpserted = Math.max(0, totalUpserted);
        this.totalErrors = Math.max(0, totalErrors);
        this.errorSummary = errorSummary == null ? "" : errorSummary;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public BatchExecutionStatus getStatus() {
        return status;
    }

    public int getTotalFetched() {
        return totalFetched;
    }

    public int getTotalUpserted() {
        return totalUpserted;
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public String getErrorSummary() {
        return errorSummary;
    }
}
