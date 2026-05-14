package com.animalplatform.story.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdoptionStoryLikeRepository extends JpaRepository<AdoptionStoryLike, Long> {

    Optional<AdoptionStoryLike> findByStoryIdAndUserId(Long storyId, Long userId);

    boolean existsByStoryIdAndUserId(Long storyId, Long userId);

    void deleteAllByStoryId(Long storyId);
}
