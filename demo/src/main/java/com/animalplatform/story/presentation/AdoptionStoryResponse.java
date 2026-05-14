package com.animalplatform.story.presentation;

import com.animalplatform.story.domain.AdoptionStory;
import java.time.LocalDateTime;

public record AdoptionStoryResponse(
        Long id,
        Long userId,
        String userNickname,
        String animalNo,
        String title,
        String content,
        String imageUrl,
        int likeCount,
        boolean liked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdoptionStoryResponse from(AdoptionStory story, boolean liked) {
        return new AdoptionStoryResponse(
                story.getId(),
                story.getUserId(),
                story.getUserNickname(),
                story.getAnimalNo(),
                story.getTitle(),
                story.getContent(),
                story.getImageUrl(),
                story.getLikeCount(),
                liked,
                story.getCreatedAt(),
                story.getUpdatedAt()
        );
    }
}
