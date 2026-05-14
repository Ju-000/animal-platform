package com.animalplatform.favorite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_favorites",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_favorite_animal", columnNames = {"user_id", "animal_no"})
)
public class UserFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "animal_no", nullable = false, length = 120)
    private String animalNo;

    @Column(nullable = false)
    private LocalDateTime savedAt;

    protected UserFavorite() {
    }

    public UserFavorite(Long userId, String animalNo) {
        this.userId = userId;
        this.animalNo = animalNo;
    }

    @PrePersist
    void prePersist() {
        if (savedAt == null) {
            savedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAnimalNo() {
        return animalNo;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}
