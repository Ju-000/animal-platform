package com.animalplatform.campaign.presentation;

import com.animalplatform.campaign.domain.DonationCampaign;
import java.time.LocalDateTime;

public record DonationCampaignResponse(
        Long id,
        String category,
        String title,
        String imageUrl,
        Long goalAmount,
        Long currentAmount,
        int participants,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {

    public static DonationCampaignResponse from(DonationCampaign campaign) {
        return new DonationCampaignResponse(
                campaign.getId(),
                campaign.getCategory(),
                campaign.getTitle(),
                campaign.getImageUrl(),
                campaign.getGoalAmount(),
                campaign.getCurrentAmount(),
                campaign.getParticipants(),
                campaign.isActive(),
                campaign.getCreatedAt(),
                campaign.getExpiresAt()
        );
    }
}
