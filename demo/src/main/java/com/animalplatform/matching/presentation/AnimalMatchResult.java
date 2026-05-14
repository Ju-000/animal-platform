package com.animalplatform.matching.presentation;

import java.util.List;

public record AnimalMatchResult(
        Long id,
        String desertionNo,
        String noticeNumber,
        String name,
        String species,
        String sex,
        String ageText,
        String weightText,
        String region,
        String serviceStatus,
        String imageUrl,
        List<String> imageUrls,
        Long shelterId,
        String shelterName,
        String specialMark,
        int matchScore,
        List<String> matchReasons
) {
}
