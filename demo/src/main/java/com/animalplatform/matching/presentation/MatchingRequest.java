package com.animalplatform.matching.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MatchingRequest(
        @NotNull HousingType housingType,
        @NotNull ActivityLevel activityLevel,
        @NotNull PreferredSize preferredSize,
        @NotNull PreferredSpecies preferredSpecies,
        boolean hasChildren,
        boolean hasOtherPets,
        @Min(0) @Max(12) int workHoursPerDay
) {
    public enum HousingType {
        APARTMENT,
        HOUSE,
        VILLA
    }

    public enum ActivityLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum PreferredSize {
        SMALL,
        MEDIUM,
        LARGE,
        ANY
    }

    public enum PreferredSpecies {
        DOG,
        CAT,
        ANY
    }
}
