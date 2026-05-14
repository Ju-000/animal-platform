package com.animalplatform.statistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "animal_status_histories")
public class AnimalStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String desertionNo;

    @Column(length = 100)
    private String previousStatus;

    @Column(nullable = false, length = 100)
    private String newStatus;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    protected AnimalStatusHistory() {
    }

    public AnimalStatusHistory(String desertionNo, String previousStatus, String newStatus, LocalDateTime detectedAt) {
        this.desertionNo = desertionNo;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.detectedAt = detectedAt;
    }

    public Long getId() {
        return id;
    }

    public String getDesertionNo() {
        return desertionNo;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }
}
