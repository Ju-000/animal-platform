package com.animalplatform.chat.presentation;

import jakarta.validation.constraints.NotBlank;

public record QuickAnswerRequest(@NotBlank String keyword) {
}
