package com.animalplatform.payment.domain;

import com.animalplatform.donation.domain.DonationTargetType;
import com.animalplatform.donation.domain.DonationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "prepared_payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_prepared_payment_merchant_uid", columnNames = "merchant_uid")
)
public class PreparedPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_uid", nullable = false, length = 80)
    private String merchantUid;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationType donationType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String orderName;

    @Column(nullable = false, length = 100)
    private String buyerName;

    @Column(nullable = false, length = 255)
    private String buyerEmail;

    @Column(nullable = false, length = 30)
    private String buyerTel;

    @Column(length = 20)
    private String subscriptionInterval;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected PreparedPayment() {
    }

    public PreparedPayment(
            String merchantUid,
            Long userId,
            DonationTargetType targetType,
            DonationType donationType,
            Long targetId,
            BigDecimal amount,
            String orderName,
            String buyerName,
            String buyerEmail,
            String buyerTel,
            String subscriptionInterval,
            LocalDateTime expiresAt
    ) {
        this.merchantUid = merchantUid;
        this.userId = userId;
        this.targetType = targetType;
        this.donationType = donationType;
        this.targetId = targetId;
        this.amount = amount;
        this.orderName = orderName;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.buyerTel = buyerTel;
        this.subscriptionInterval = subscriptionInterval;
        this.expiresAt = expiresAt;
    }

    public String getMerchantUid() {
        return merchantUid;
    }

    public Long getUserId() {
        return userId;
    }

    public DonationTargetType getTargetType() {
        return targetType;
    }

    public DonationType getDonationType() {
        return donationType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getOrderName() {
        return orderName;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public String getBuyerTel() {
        return buyerTel;
    }

    public String getSubscriptionInterval() {
        return subscriptionInterval;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
