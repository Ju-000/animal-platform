package com.animalplatform.donation.domain;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionPaymentHistoryRepository extends JpaRepository<SubscriptionPaymentHistory, Long> {

    List<SubscriptionPaymentHistory> findBySubscriptionDonationIdOrderByCreatedAtDesc(Long subscriptionDonationId);

    @Query(value = """
            select history
            from SubscriptionPaymentHistory history
            join fetch history.subscriptionDonation subscription
            join fetch subscription.user
            left join fetch subscription.shelter
            left join fetch subscription.animal
            """,
            countQuery = "select count(history) from SubscriptionPaymentHistory history")
    Page<SubscriptionPaymentHistory> findAdminPage(Pageable pageable);

    @Query("""
            select coalesce(sum(history.amount), 0)
            from SubscriptionPaymentHistory history
            where history.subscriptionDonation.user.id = :userId
              and history.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
            """)
    BigDecimal sumPaidAmountByUserId(@Param("userId") Long userId);

    @Query("""
            select coalesce(sum(history.amount), 0)
            from SubscriptionPaymentHistory history
            where history.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
              and history.paidAt >= :start
              and history.paidAt < :end
            """)
    BigDecimal sumPaidAmountByPaidAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select history
            from SubscriptionPaymentHistory history
            join fetch history.subscriptionDonation subscription
            left join fetch subscription.shelter
            left join fetch subscription.animal
            where subscription.user.id = :userId
              and history.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
            order by history.paidAt desc
            """)
    List<SubscriptionPaymentHistory> findPaidHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select coalesce(sum(history.amount), 0)
            from SubscriptionPaymentHistory history
            where history.subscriptionDonation.shelter.id = :shelterId
              and history.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
              and history.paidAt >= :start
              and history.paidAt < :end
            """)
    BigDecimal sumPaidAmountByShelterIdAndPaidAtBetween(
            @Param("shelterId") Long shelterId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
