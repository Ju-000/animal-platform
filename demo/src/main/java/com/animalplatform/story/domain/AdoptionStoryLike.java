package com.animalplatform.story.domain;

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
        name = "adoption_story_likes",
        uniqueConstraints = @UniqueConstraint(name = "uq_adoption_story_like_user", columnNames = {"story_id", "user_id"})
)
public class AdoptionStoryLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime likedAt;

    protected AdoptionStoryLike() {
    }

    public AdoptionStoryLike(Long storyId, Long userId) {
        this.storyId = storyId;
        this.userId = userId;
    }

    @PrePersist
    void prePersist() {
        if (likedAt == null) {
            likedAt = LocalDateTime.now();
        }
    }
}
