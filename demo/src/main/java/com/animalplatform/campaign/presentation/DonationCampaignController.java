package com.animalplatform.campaign.presentation;

import com.animalplatform.campaign.application.DonationCampaignService;
import com.animalplatform.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DonationCampaignController {

    private final DonationCampaignService donationCampaignService;

    public DonationCampaignController(DonationCampaignService donationCampaignService) {
        this.donationCampaignService = donationCampaignService;
    }

    @GetMapping("/api/campaigns")
    public ApiResponse<List<DonationCampaignResponse>> getActiveCampaigns() {
        return ApiResponse.ok(donationCampaignService.getActiveCampaigns());
    }

    @GetMapping("/api/admin/campaigns")
    public ApiResponse<List<DonationCampaignResponse>> getAllCampaigns() {
        return ApiResponse.ok(donationCampaignService.getAllCampaigns());
    }

    @PostMapping("/api/admin/campaigns")
    public ApiResponse<DonationCampaignResponse> createCampaign(@RequestBody DonationCampaignRequest request) {
        return ApiResponse.ok(donationCampaignService.createCampaign(request));
    }

    @PutMapping("/api/admin/campaigns/{id}")
    public ApiResponse<DonationCampaignResponse> updateCampaign(
            @PathVariable Long id,
            @RequestBody DonationCampaignRequest request
    ) {
        return ApiResponse.ok(donationCampaignService.updateCampaign(id, request));
    }

    @DeleteMapping("/api/admin/campaigns/{id}")
    public ApiResponse<Void> deleteCampaign(@PathVariable Long id) {
        donationCampaignService.deleteCampaign(id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/admin/campaigns/{id}/toggle")
    public ApiResponse<DonationCampaignResponse> toggleCampaign(@PathVariable Long id) {
        return ApiResponse.ok(donationCampaignService.toggleActive(id));
    }

    @PostMapping("/api/admin/campaigns/{id}/image")
    public ApiResponse<DonationCampaignResponse> uploadCampaignImage(
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.ok(donationCampaignService.uploadImage(id, image));
    }
}
