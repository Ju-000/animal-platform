package com.animalplatform.shelter.domain;

import com.animalplatform.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shelters")
public class Shelter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String externalShelterCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String regionCode;

    @Column(length = 255)
    private String homepageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    protected Shelter() {
    }

    public Shelter(String externalShelterCode, String name, String phone, String address, String regionCode,
                   String homepageUrl, String description) {
        this.externalShelterCode = externalShelterCode;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.regionCode = regionCode;
        this.homepageUrl = homepageUrl;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getExternalShelterCode() {
        return externalShelterCode;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getRegionCode() {
        return regionCode;
    }
}
