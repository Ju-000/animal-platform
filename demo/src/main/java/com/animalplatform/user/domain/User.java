package com.animalplatform.user.domain;

import com.animalplatform.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 20)
    private String birthDate;

    @Column(length = 20)
    private String gender;

    @Column(length = 500)
    private String address;

    @Column(nullable = false)
    private boolean privacyConsent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(nullable = false)
    private boolean allowEmailNotification = true;

    @Column(nullable = false)
    private boolean urgentAnimalAlert = true;

    @Column(nullable = false)
    private boolean monthlyNewsletter = true;

    @Column(nullable = false)
    private boolean weeklyNewsletter = true;

    protected User() {
    }

    public User(UserRole role, String username, String email, String passwordHash, String name, String phone, UserStatus status) {
        this(role, username, email, passwordHash, name, phone, null, null, null, false, status);
    }

    public User(
            UserRole role,
            String username,
            String email,
            String passwordHash,
            String name,
            String phone,
            String birthDate,
            String gender,
            String address,
            boolean privacyConsent,
            UserStatus status
    ) {
        this.role = role;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.address = address;
        this.privacyConsent = privacyConsent;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public String getAddress() {
        return address;
    }

    public boolean isPrivacyConsent() {
        return privacyConsent;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isAllowEmailNotification() {
        return allowEmailNotification;
    }

    public boolean isUrgentAnimalAlert() {
        return urgentAnimalAlert;
    }

    public boolean isMonthlyNewsletter() {
        return monthlyNewsletter;
    }

    public boolean isWeeklyNewsletter() {
        return weeklyNewsletter;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void updateNotificationSettings(boolean urgentAnimalAlert, boolean monthlyNewsletter, boolean weeklyNewsletter) {
        this.urgentAnimalAlert = urgentAnimalAlert;
        this.monthlyNewsletter = monthlyNewsletter;
        this.weeklyNewsletter = weeklyNewsletter;
    }
}
