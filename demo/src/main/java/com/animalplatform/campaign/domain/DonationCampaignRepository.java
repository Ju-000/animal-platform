package com.animalplatform.campaign.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonationCampaignRepository extends JpaRepository<DonationCampaign, Long> {

    List<DonationCampaign> findByActiveTrueOrderByCreatedAtDesc();

    boolean existsByTitle(String title);

    @Query("""
            select coalesce(sum(campaign.currentAmount), 0)
            from DonationCampaign campaign
            where campaign.active = true
              and campaign.category like concat('%', :keyword, '%')
            """)
    Long sumActiveCurrentAmountByCategoryKeyword(@Param("keyword") String keyword);
}
