package com.animalplatform.campaign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation_campaigns")
public class DonationCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private Long goalAmount;

    @Column(nullable = false)
    private Long currentAmount;

    @Column(nullable = false)
    private int participants;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    protected DonationCampaign() {
    }

    public DonationCampaign(
            String category,
            String title,
            String imageUrl,
            Long goalAmount,
            Long currentAmount,
            int participants,
            boolean active,
            LocalDateTime expiresAt
    ) {
        this.category = category;
        this.title = title;
        this.imageUrl = imageUrl;
        this.goalAmount = goalAmount;
        this.currentAmount = currentAmount == null ? 0L : currentAmount;
        this.participants = participants;
        this.active = active;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (currentAmount == null) {
            currentAmount = 0L;
        }
        if (goalAmount == null) {
            goalAmount = 0L;
        }
    }

    public void update(
            String category,
            String title,
            String imageUrl,
            Long goalAmount,
            Long currentAmount,
            int participants,
            Boolean active,
            LocalDateTime expiresAt
    ) {
        this.category = category;
        this.title = title;
        this.imageUrl = imageUrl;
        this.goalAmount = goalAmount == null ? 0L : goalAmount;
        this.currentAmount = currentAmount == null ? this.currentAmount : currentAmount;
        this.participants = participants;
        if (active != null) {
            this.active = active;
        }
        this.expiresAt = expiresAt;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void toggleActive() {
        this.active = !this.active;
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getGoalAmount() {
        return goalAmount;
    }

    public Long getCurrentAmount() {
        return currentAmount;
    }

    public int getParticipants() {
        return participants;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
