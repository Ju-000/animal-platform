package com.animalplatform.notification.application;

import com.animalplatform.favorite.domain.UserFavorite;
import com.animalplatform.favorite.domain.UserFavoriteRepository;
import com.animalplatform.statistics.application.BatchCollectorService.StatusChangeEvent;
import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String SUBJECT = "[포동포동] 관심 동물의 상태가 변경되었습니다";

    private final UserFavoriteRepository userFavoriteRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final String frontendBaseUrl;
    private final String mailUsername;

    public NotificationService(
            UserFavoriteRepository userFavoriteRepository,
            UserRepository userRepository,
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.userFavoriteRepository = userFavoriteRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.frontendBaseUrl = frontendBaseUrl;
        this.mailUsername = mailUsername;
    }

    public void notifyFavoriteStatusChanges(List<StatusChangeEvent> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        Map<String, StatusChangeEvent> changesByFavoriteKey = buildChangesByFavoriteKey(changes);
        List<UserFavorite> favorites = userFavoriteRepository.findAllByAnimalNoIn(new ArrayList<>(changesByFavoriteKey.keySet()));
        if (favorites.isEmpty()) {
            return;
        }

        Map<Long, User> usersById = userRepository.findAllById(
                        favorites.stream().map(UserFavorite::getUserId).collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        for (UserFavorite favorite : favorites) {
            User user = usersById.get(favorite.getUserId());
            StatusChangeEvent change = changesByFavoriteKey.get(favorite.getAnimalNo());
            if (user == null || change == null || shouldSkip(user)) {
                continue;
            }

            sendStatusChangeEmail(user, change);
        }
    }

    private Map<String, StatusChangeEvent> buildChangesByFavoriteKey(List<StatusChangeEvent> changes) {
        Map<String, StatusChangeEvent> result = new LinkedHashMap<>();
        for (StatusChangeEvent change : changes) {
            if (StringUtils.hasText(change.desertionNo())) {
                result.put(change.desertionNo(), change);
            }
            if (StringUtils.hasText(change.noticeNo())) {
                result.put(change.noticeNo(), change);
            }
        }
        return result;
    }

    private boolean shouldSkip(User user) {
        return !user.isAllowEmailNotification() || !StringUtils.hasText(user.getEmail());
    }

    private void sendStatusChangeEmail(User user, StatusChangeEvent change) {
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
            helper.setSubject(SUBJECT);
            helper.setText(renderTemplate(user, change), true);

            mailSender.send(message);
            log.info("Sent favorite status change email. userId={}, animalNo={}", user.getId(), change.desertionNo());
        } catch (Exception exception) {
            log.error(
                    "Failed to send favorite status change email. userId={}, animalNo={}",
                    user.getId(),
                    change.desertionNo(),
                    exception
            );
        }
    }

    private String renderTemplate(User user, StatusChangeEvent change) {
        Context context = new Context();
        context.setVariable("userName", user.getName());
        context.setVariable("animalName", fallback(change.animalName(), "관심 동물"));
        context.setVariable("thumbnailUrl", change.thumbnailUrl());
        context.setVariable("previousStatus", fallback(change.previousStatus(), "이전 상태 없음"));
        context.setVariable("newStatus", fallback(change.newStatus(), "상태 확인 필요"));
        context.setVariable("newStatusClass", statusClass(change.newStatus()));
        context.setVariable("detailUrl", animalDetailUrl(change));
        return templateEngine.process("mail/favorite-status-change", context);
    }

    private String animalDetailUrl(StatusChangeEvent change) {
        String lookupToken = StringUtils.hasText(change.noticeNo()) ? change.noticeNo() : change.desertionNo();
        return UriComponentsBuilder
                .fromUriString(frontendBaseUrl)
                .path("/shelter/animal/detail/{animalNo}")
                .buildAndExpand(lookupToken)
                .toUriString();
    }

    private String statusClass(String status) {
        if (status == null) {
            return "neutral";
        }
        if (status.contains("안락사")) {
            return "danger";
        }
        if (status.contains("입양") || status.contains("분양")) {
            return "success";
        }
        return "neutral";
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
