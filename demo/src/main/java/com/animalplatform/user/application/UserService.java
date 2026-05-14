package com.animalplatform.user.application;

import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.SubscriptionPaymentHistoryRepository;
import com.animalplatform.user.domain.DonorTier;
import com.animalplatform.user.domain.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final DonationRepository donationRepository;
    private final SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository;

    public UserService(
            DonationRepository donationRepository,
            SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository
    ) {
        this.donationRepository = donationRepository;
        this.subscriptionPaymentHistoryRepository = subscriptionPaymentHistoryRepository;
    }

    @Transactional(readOnly = true)
    public DonorTier getTier(User user) {
        return DonorTier.fromTotalAmount(getTotalDonatedAmount(user));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildTierPayload(User user) {
        long totalDonatedAmount = getTotalDonatedAmount(user);
        DonorTier tier = DonorTier.fromTotalAmount(totalDonatedAmount);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tier", tier.name());
        payload.put("tierLabel", tier.getLabel());
        payload.put("tierEmoji", tier.getEmoji());
        payload.put("totalDonatedAmount", totalDonatedAmount);
        payload.put("nextTierAmount", tier.nextTierAmount(totalDonatedAmount));
        payload.put("tierMinimumAmount", tier.getMinimumAmount());
        payload.put("nextTierMinimumAmount", tier.getNextMinimumAmount());
        return payload;
    }

    private long getTotalDonatedAmount(User user) {
        BigDecimal oneTime = donationRepository.sumPaidAmountByUserId(user.getId());
        BigDecimal recurring = subscriptionPaymentHistoryRepository.sumPaidAmountByUserId(user.getId());
        return oneTime.add(recurring).setScale(0, RoundingMode.DOWN).longValue();
    }
}
