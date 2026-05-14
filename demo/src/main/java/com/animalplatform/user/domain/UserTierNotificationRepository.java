package com.animalplatform.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTierNotificationRepository extends JpaRepository<UserTierNotification, Long> {

    boolean existsByUserIdAndTier(Long userId, DonorTier tier);
}
