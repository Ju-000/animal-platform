package com.animalplatform.donation.domain;

import com.animalplatform.animal.domain.Animal;
import com.animalplatform.common.entity.BaseTimeEntity;
import com.animalplatform.shelter.domain.Shelter;
import com.animalplatform.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_donations")
public class SubscriptionDonation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelter_id")
    private Shelter shelter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionInterval subscriptionInterval;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionDonationStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String billingKey;

    private LocalDateTime nextBillingAt;

    protected SubscriptionDonation() {
    }

    public SubscriptionDonation(
            User user,
            Shelter shelter,
            Animal animal,
            DonationTargetType targetType,
            SubscriptionInterval subscriptionInterval,
            SubscriptionDonationStatus status,
            BigDecimal amount,
            String billingKey,
            LocalDateTime nextBillingAt
    ) {
        this.user = user;
        this.shelter = shelter;
        this.animal = animal;
        this.targetType = targetType;
        this.subscriptionInterval = subscriptionInterval;
        this.status = status;
        this.amount = amount;
        this.billingKey = billingKey;
        this.nextBillingAt = nextBillingAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Shelter getShelter() {
        return shelter;
    }

    public Animal getAnimal() {
        return animal;
    }

    public DonationTargetType getTargetType() {
        return targetType;
    }

    public SubscriptionInterval getSubscriptionInterval() {
        return subscriptionInterval;
    }

    public SubscriptionDonationStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getNextBillingAt() {
        return nextBillingAt;
    }
}
