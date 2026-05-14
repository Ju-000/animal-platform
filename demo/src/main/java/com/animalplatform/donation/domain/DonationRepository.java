package com.animalplatform.donation.domain;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByUserId(Long userId);

    List<Donation> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByPaymentKey(String paymentKey);

    @Query(value = """
            select d
            from Donation d
            join fetch d.user
            left join fetch d.shelter
            left join fetch d.animal
            """,
            countQuery = "select count(d) from Donation d")
    Page<Donation> findAdminPage(Pageable pageable);

    @Query("""
            select coalesce(sum(d.amount), 0)
            from Donation d
            where d.user.id = :userId
              and d.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
            """)
    BigDecimal sumPaidAmountByUserId(@Param("userId") Long userId);

    @Query("""
            select min(d.paidAt)
            from Donation d
            where d.user.id = :userId
              and d.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
            """)
    LocalDateTime findFirstPaidAtByUserId(@Param("userId") Long userId);

    @Query("""
            select coalesce(sum(d.amount), 0)
            from Donation d
            where d.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
              and d.paidAt >= :start
              and d.paidAt < :end
            """)
    BigDecimal sumPaidAmountByPaidAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select d
            from Donation d
            left join fetch d.shelter
            left join fetch d.animal
            where d.user.id = :userId
              and d.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
            order by d.paidAt desc
            """)
    List<Donation> findPaidHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select coalesce(sum(d.amount), 0)
            from Donation d
            where d.shelter.id = :shelterId
              and d.paymentStatus = com.animalplatform.donation.domain.DonationStatus.PAID
              and d.paidAt >= :start
              and d.paidAt < :end
            """)
    BigDecimal sumPaidAmountByShelterIdAndPaidAtBetween(
            @Param("shelterId") Long shelterId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
