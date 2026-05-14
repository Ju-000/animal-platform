package com.animalplatform.campaign.presentation;

import java.time.LocalDateTime;

public record DonationCampaignRequest(
        String category,
        String title,
        String imageUrl,
        Long goalAmount,
        Long currentAmount,
        Integer participants,
        Boolean active,
        LocalDateTime expiresAt
) {
}
