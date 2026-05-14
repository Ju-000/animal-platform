package com.animalplatform.animal.domain;

import com.animalplatform.common.entity.BaseTimeEntity;
import com.animalplatform.shelter.domain.Shelter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "animals")
public class Animal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 120)
    private String publicApiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelter_id")
    private Shelter shelter;

    @Column(nullable = false, length = 50)
    private String species;

    @Column(length = 150)
    private String breed;

    @Column(length = 20)
    private String sex;

    @Column(length = 100)
    private String ageText;

    @Column(length = 50)
    private String weightText;

    @Column(length = 100)
    private String colorText;

    @Column(length = 20)
    private String neuterStatus;

    @Column(columnDefinition = "TEXT")
    private String specialMark;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 255)
    private String foundPlace;

    private LocalDate rescueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnimalStatus serviceStatus;

    protected Animal() {
    }

    public Animal(String publicApiId, Shelter shelter, String species, String breed, String sex, String ageText,
                  String weightText, String colorText, String neuterStatus, String specialMark, String imageUrl,
                  String foundPlace, LocalDate rescueDate, AnimalStatus serviceStatus) {
        this.publicApiId = publicApiId;
        this.shelter = shelter;
        this.species = species;
        this.breed = breed;
        this.sex = sex;
        this.ageText = ageText;
        this.weightText = weightText;
        this.colorText = colorText;
        this.neuterStatus = neuterStatus;
        this.specialMark = specialMark;
        this.imageUrl = imageUrl;
        this.foundPlace = foundPlace;
        this.rescueDate = rescueDate;
        this.serviceStatus = serviceStatus;
    }

    public Long getId() {
        return id;
    }

    public String getPublicApiId() {
        return publicApiId;
    }

    public String getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public String getSex() {
        return sex;
    }

    public String getAgeText() {
        return ageText;
    }

    public String getWeightText() {
        return weightText;
    }

    public String getFoundPlace() {
        return foundPlace;
    }

    public AnimalStatus getServiceStatus() {
        return serviceStatus;
    }

    public Shelter getShelter() {
        return shelter;
    }

    public String getDisplayName() {
        if (breed != null && !breed.isBlank()) {
            return breed;
        }

        return species;
    }

    public String getName() {
        return getDisplayName();
    }
}
