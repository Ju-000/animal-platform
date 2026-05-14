package com.animalplatform.campaign.application;

import com.animalplatform.campaign.domain.DonationCampaign;
import com.animalplatform.campaign.domain.DonationCampaignRepository;
import com.animalplatform.campaign.presentation.DonationCampaignRequest;
import com.animalplatform.campaign.presentation.DonationCampaignResponse;
import com.animalplatform.storage.StorageService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DonationCampaignService {

    private final DonationCampaignRepository donationCampaignRepository;
    private final StorageService storageService;

    public DonationCampaignService(
            DonationCampaignRepository donationCampaignRepository,
            StorageService storageService
    ) {
        this.donationCampaignRepository = donationCampaignRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<DonationCampaignResponse> getActiveCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        return donationCampaignRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(campaign -> campaign.getExpiresAt() == null || campaign.getExpiresAt().isAfter(now))
                .map(DonationCampaignResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DonationCampaignResponse> getAllCampaigns() {
        return donationCampaignRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(DonationCampaignResponse::from)
                .toList();
    }

    @Transactional
    public DonationCampaignResponse createCampaign(DonationCampaignRequest request) {
        DonationCampaign campaign = new DonationCampaign(
                required(request.category(), "category"),
                required(request.title(), "title"),
                clean(request.imageUrl()),
                positiveAmount(request.goalAmount()),
                request.currentAmount() == null ? 0L : Math.max(0L, request.currentAmount()),
                request.participants() == null ? 0 : Math.max(0, request.participants()),
                request.active() == null || request.active(),
                request.expiresAt()
        );
        return DonationCampaignResponse.from(donationCampaignRepository.save(campaign));
    }

    @Transactional
    public DonationCampaignResponse updateCampaign(Long id, DonationCampaignRequest request) {
        DonationCampaign campaign = getCampaign(id);
        campaign.update(
                required(request.category(), "category"),
                required(request.title(), "title"),
                clean(request.imageUrl()),
                positiveAmount(request.goalAmount()),
                request.currentAmount() == null ? campaign.getCurrentAmount() : Math.max(0L, request.currentAmount()),
                request.participants() == null ? campaign.getParticipants() : Math.max(0, request.participants()),
                request.active(),
                request.expiresAt()
        );
        return DonationCampaignResponse.from(campaign);
    }

    @Transactional
    public void deleteCampaign(Long id) {
        DonationCampaign campaign = getCampaign(id);
        if (StringUtils.hasText(campaign.getImageUrl())) {
            storageService.delete(campaign.getImageUrl());
        }
        donationCampaignRepository.delete(campaign);
    }

    @Transactional
    public DonationCampaignResponse toggleActive(Long id) {
        DonationCampaign campaign = getCampaign(id);
        campaign.toggleActive();
        return DonationCampaignResponse.from(campaign);
    }

    @Transactional
    public DonationCampaignResponse uploadImage(Long id, MultipartFile file) {
        DonationCampaign campaign = getCampaign(id);
        String oldImageUrl = campaign.getImageUrl();
        String imageUrl = storageService.store(file, "campaigns");
        campaign.updateImageUrl(imageUrl);
        if (StringUtils.hasText(oldImageUrl)) {
            storageService.delete(oldImageUrl);
        }
        return DonationCampaignResponse.from(campaign);
    }

    private DonationCampaign getCampaign(Long id) {
        return donationCampaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation campaign not found."));
    }

    private String required(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        return value.trim();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long positiveAmount(Long value) {
        if (value == null || value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "goalAmount must be greater than 0.");
        }
        return value;
    }
}
