package com.animalplatform.donation.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionDonationRepository extends JpaRepository<SubscriptionDonation, Long> {

    List<SubscriptionDonation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
