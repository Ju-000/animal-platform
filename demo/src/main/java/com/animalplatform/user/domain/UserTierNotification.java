package com.animalplatform.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_tier_notifications",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_tier_notification", columnNames = {"user_id", "tier"})
)
public class UserTierNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DonorTier tier;

    @Column(nullable = false)
    private LocalDateTime notifiedAt;

    protected UserTierNotification() {
    }

    public UserTierNotification(Long userId, DonorTier tier, LocalDateTime notifiedAt) {
        this.userId = userId;
        this.tier = tier;
        this.notifiedAt = notifiedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public DonorTier getTier() {
        return tier;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }
}
