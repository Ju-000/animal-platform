package com.animalplatform.donation.domain;

import com.animalplatform.common.entity.BaseTimeEntity;
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
@Table(name = "subscription_payment_histories")
public class SubscriptionPaymentHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_donation_id", nullable = false)
    private SubscriptionDonation subscriptionDonation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus paymentStatus;

    @Column(length = 120)
    private String paymentKey;

    private LocalDateTime paidAt;

    protected SubscriptionPaymentHistory() {
    }

    public SubscriptionPaymentHistory(
            SubscriptionDonation subscriptionDonation,
            BigDecimal amount,
            DonationStatus paymentStatus,
            String paymentKey,
            LocalDateTime paidAt
    ) {
        this.subscriptionDonation = subscriptionDonation;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.paymentKey = paymentKey;
        this.paidAt = paidAt;
    }

    public Long getId() {
        return id;
    }

    public SubscriptionDonation getSubscriptionDonation() {
        return subscriptionDonation;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public DonationStatus getPaymentStatus() {
        return paymentStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
