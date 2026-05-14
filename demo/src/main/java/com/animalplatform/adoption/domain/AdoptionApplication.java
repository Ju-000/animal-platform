package com.animalplatform.adoption.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "adoption_applications")
public class AdoptionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String animalNo;

    @Column(nullable = false, length = 100)
    private String applicantName;

    @Column(nullable = false, length = 30)
    private String applicantPhone;

    @Column(nullable = false, length = 255)
    private String applicantEmail;

    @Column(nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HousingType housingType;

    @Column(nullable = false)
    private boolean hasExperience;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdoptionApplicationStatus status = AdoptionApplicationStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    private Long userId;

    protected AdoptionApplication() {
    }

    public AdoptionApplication(
            String animalNo,
            String applicantName,
            String applicantPhone,
            String applicantEmail,
            String address,
            HousingType housingType,
            boolean hasExperience,
            String reason,
            Long userId
    ) {
        this.animalNo = animalNo;
        this.applicantName = applicantName;
        this.applicantPhone = applicantPhone;
        this.applicantEmail = applicantEmail;
        this.address = address;
        this.housingType = housingType;
        this.hasExperience = hasExperience;
        this.reason = reason;
        this.userId = userId;
    }

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = AdoptionApplicationStatus.PENDING;
        }
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getAnimalNo() {
        return animalNo;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public String getAddress() {
        return address;
    }

    public HousingType getHousingType() {
        return housingType;
    }

    public boolean isHasExperience() {
        return hasExperience;
    }

    public String getReason() {
        return reason;
    }

    public AdoptionApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void changeStatus(AdoptionApplicationStatus status) {
        this.status = status;
    }
}
