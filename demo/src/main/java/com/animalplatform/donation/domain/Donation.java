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
@Table(name = "donations")
public class Donation extends BaseTimeEntity {

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
    private DonationType donationType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus paymentStatus;

    @Column(length = 120)
    private String paymentKey;

    private LocalDateTime paidAt;

    protected Donation() {
    }

    public Donation(User user, Shelter shelter, Animal animal, DonationTargetType targetType,
                    DonationType donationType,
                    BigDecimal amount, DonationStatus paymentStatus, String paymentKey, LocalDateTime paidAt) {
        this.user = user;
        this.shelter = shelter;
        this.animal = animal;
        this.targetType = targetType;
        this.donationType = donationType;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.paymentKey = paymentKey;
        this.paidAt = paidAt;
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

    public DonationType getDonationType() {
        return donationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public DonationStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void markPaid(String paymentKey, LocalDateTime paidAt) {
        this.paymentStatus = DonationStatus.PAID;
        this.paymentKey = paymentKey;
        this.paidAt = paidAt;
    }
}
