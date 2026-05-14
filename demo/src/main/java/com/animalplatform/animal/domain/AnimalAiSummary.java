package com.animalplatform.animal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "animal_ai_summaries")
public class AnimalAiSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String desertionNo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected AnimalAiSummary() {
    }

    public AnimalAiSummary(String desertionNo, String summaryText, LocalDateTime generatedAt, LocalDateTime expiresAt) {
        this.desertionNo = desertionNo;
        this.summaryText = summaryText;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getDesertionNo() {
        return desertionNo;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void refresh(String summaryText, LocalDateTime generatedAt, LocalDateTime expiresAt) {
        this.summaryText = summaryText;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }
}
