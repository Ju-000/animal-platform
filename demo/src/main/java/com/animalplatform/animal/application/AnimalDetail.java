package com.animalplatform.animal.application;

public record AnimalDetail(
        String desertionNo,
        String kindCd,
        String colorCd,
        String age,
        String weight,
        String sexCd,
        String neuterYn,
        String specialMark,
        String processState
) {
}
