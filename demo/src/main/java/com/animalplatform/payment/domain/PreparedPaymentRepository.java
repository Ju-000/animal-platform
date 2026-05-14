package com.animalplatform.payment.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreparedPaymentRepository extends JpaRepository<PreparedPayment, Long> {

    Optional<PreparedPayment> findByMerchantUid(String merchantUid);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
