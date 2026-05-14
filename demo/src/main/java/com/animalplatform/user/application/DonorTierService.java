package com.animalplatform.user.application;

import com.animalplatform.campaign.domain.DonationCampaign;
import com.animalplatform.campaign.domain.DonationCampaignRepository;
import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.SubscriptionPaymentHistoryRepository;
import com.animalplatform.statistics.domain.AnimalSnapshot;
import com.animalplatform.statistics.domain.SnapshotRepository;
import com.animalplatform.user.domain.DonorTier;
import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import com.animalplatform.user.domain.UserTierNotification;
import com.animalplatform.user.domain.UserTierNotificationRepository;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class DonorTierService {

    private static final Logger log = LoggerFactory.getLogger(DonorTierService.class);
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository;
    private final UserTierNotificationRepository notificationRepository;
    private final DonationCampaignRepository campaignRepository;
    private final SnapshotRepository snapshotRepository;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String frontendBaseUrl;
    private final String mailUsername;

    public DonorTierService(
            UserRepository userRepository,
            DonationRepository donationRepository,
            SubscriptionPaymentHistoryRepository subscriptionPaymentHistoryRepository,
            UserTierNotificationRepository notificationRepository,
            DonationCampaignRepository campaignRepository,
            SnapshotRepository snapshotRepository,
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.userRepository = userRepository;
        this.donationRepository = donationRepository;
        this.subscriptionPaymentHistoryRepository = subscriptionPaymentHistoryRepository;
        this.notificationRepository = notificationRepository;
        this.campaignRepository = campaignRepository;
        this.snapshotRepository = snapshotRepository;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.frontendBaseUrl = frontendBaseUrl;
        this.mailUsername = mailUsername;
    }

    @Transactional
    public void checkAndNotifyTierChange(User user, long previousTotalAmount, long currentTotalAmount) {
        if (currentTotalAmount <= 0) {
            return;
        }

        if (previousTotalAmount <= 0) {
            notifyTierIfNeeded(user, DonorTier.SPROUT);
        }

        DonorTier previousTier = DonorTier.fromTotalAmount(previousTotalAmount);
        DonorTier currentTier = DonorTier.fromTotalAmount(currentTotalAmount);
        for (DonorTier tier : DonorTier.values()) {
            if (tier.ordinal() > previousTier.ordinal() && tier.ordinal() <= currentTier.ordinal()) {
                notifyTierIfNeeded(user, tier);
            }
        }
    }

    @Scheduled(cron = "0 0 9 1 * *")
    @Transactional(readOnly = true)
    public void sendMonthlyNewsletter() {
        List<DonationCampaign> campaigns = campaignRepository.findByActiveTrueOrderByCreatedAtDesc();
        long adoptedCount = snapshotRepository.countByProcessStateContaining("입양");
        for (User user : eligibleUsers(DonorTier.SUPPORTER)) {
            if (!user.isMonthlyNewsletter()) {
                continue;
            }
            Context context = baseContext(user);
            context.setVariable("adoptedCount", adoptedCount);
            context.setVariable("campaigns", campaigns);
            sendTemplateEmail(user, "[다시, 가족] 보호소 월간 소식이 도착했어요 💛", "mail/monthly-newsletter", context);
        }
    }

    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional(readOnly = true)
    public void sendWeeklyNewsletter() {
        long adoptedCount = snapshotRepository.countByProcessStateContaining("입양");
        for (User user : eligibleUsers(DonorTier.ANGEL)) {
            if (!user.isWeeklyNewsletter()) {
                continue;
            }
            Context context = baseContext(user);
            context.setVariable("adoptedCount", adoptedCount);
            sendTemplateEmail(user, "[다시, 가족] 천사 후원자 주간 소식입니다 👼", "mail/weekly-newsletter", context);
        }
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void checkUrgentAnimalsAndNotify() {
        LocalDate today = LocalDate.now();
        List<AnimalSnapshot> urgentAnimals = snapshotRepository.findUrgentAnimals(
                today.format(BASIC_DATE),
                today.plusDays(3).format(BASIC_DATE),
                org.springframework.data.domain.PageRequest.of(0, 5)
        );
        if (urgentAnimals.isEmpty()) {
            return;
        }

        AnimalSnapshot animal = urgentAnimals.get(0);
        for (User user : eligibleUsers(DonorTier.CHAMPION)) {
            if (!user.isUrgentAnimalAlert()) {
                continue;
            }
            Context context = baseContext(user);
            context.setVariable("animal", animal);
            context.setVariable("detailUrl", animalDetailUrl(animal));
            sendTemplateEmail(user, "[다시, 가족] 긴급 구조가 필요한 아이가 있어요", "mail/urgent-animal-alert", context);
        }
    }

    private void notifyTierIfNeeded(User user, DonorTier tier) {
        if (notificationRepository.existsByUserIdAndTier(user.getId(), tier)) {
            return;
        }

        String template = switch (tier) {
            case SPROUT -> "mail/welcome-sprout";
            case SUPPORTER -> "mail/tier-up-supporter";
            case CHAMPION -> "mail/tier-up-champion";
            case ANGEL -> "mail/tier-up-angel";
        };
        String subject = switch (tier) {
            case SPROUT -> "[다시, 가족] 새싹 후원자가 되셨어요! 🌱";
            case SUPPORTER -> "[다시, 가족] 든든한 후원자가 되셨어요! 💛";
            case CHAMPION -> "[다시, 가족] 챔피언 후원자가 되셨어요! 🧡";
            case ANGEL -> "[다시, 가족] 천사 후원자가 되셨어요! 👼";
        };

        Context context = baseContext(user);
        context.setVariable("tier", tier);
        context.setVariable("totalDonatedAmount", totalDonatedAmount(user));
        context.setVariable("helpedAnimalCount", Math.max(1, totalDonatedAmount(user) / 10_000L));
        sendTemplateEmail(user, subject, template, context);
        notificationRepository.save(new UserTierNotification(user.getId(), tier, LocalDateTime.now()));
    }

    private List<User> eligibleUsers(DonorTier minimumTier) {
        return userRepository.findAllByAllowEmailNotificationTrue()
                .stream()
                .filter(user -> StringUtils.hasText(user.getEmail()))
                .filter(user -> DonorTier.fromTotalAmount(totalDonatedAmount(user)).ordinal() >= minimumTier.ordinal())
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    private Context baseContext(User user) {
        Context context = new Context();
        context.setVariable("userName", user.getName());
        context.setVariable("frontendBaseUrl", frontendBaseUrl);
        return context;
    }

    private void sendTemplateEmail(User user, String subject, String template, Context context) {
        if (!user.isAllowEmailNotification() || !StringUtils.hasText(user.getEmail())) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            if (StringUtils.hasText(mailUsername)) {
                helper.setFrom(mailUsername);
            }
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(templateEngine.process(template, context), true);
            mailSender.send(message);
            log.info("Sent donor tier email. userId={}, subject={}", user.getId(), subject);
        } catch (Exception exception) {
            log.warn("Failed to send donor tier email. userId={}, subject={}", user.getId(), subject, exception);
        }
    }

    private long totalDonatedAmount(User user) {
        BigDecimal oneTime = donationRepository.sumPaidAmountByUserId(user.getId());
        BigDecimal recurring = subscriptionPaymentHistoryRepository.sumPaidAmountByUserId(user.getId());
        return oneTime.add(recurring).setScale(0, RoundingMode.DOWN).longValue();
    }

    private String animalDetailUrl(AnimalSnapshot animal) {
        String lookupToken = StringUtils.hasText(animal.getNoticeNo()) ? animal.getNoticeNo() : animal.getDesertionNo();
        return UriComponentsBuilder
                .fromUriString(frontendBaseUrl)
                .path("/shelter/animal/detail/{animalNo}")
                .buildAndExpand(Map.of("animalNo", lookupToken))
                .toUriString();
    }
}
