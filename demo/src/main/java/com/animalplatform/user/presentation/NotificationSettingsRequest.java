package com.animalplatform.user.presentation;

public record NotificationSettingsRequest(
        boolean urgentAnimalAlert,
        boolean monthlyNewsletter,
        boolean weeklyNewsletter
) {
}
