package com.animalplatform.story.application;

import com.animalplatform.story.domain.AdoptionStory;
import com.animalplatform.story.domain.AdoptionStoryLike;
import com.animalplatform.story.domain.AdoptionStoryLikeRepository;
import com.animalplatform.story.domain.AdoptionStoryRepository;
import com.animalplatform.story.presentation.AdoptionStoryResponse;
import com.animalplatform.storage.StorageService;
import com.animalplatform.user.domain.User;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdoptionStoryService {

    private final AdoptionStoryRepository storyRepository;
    private final AdoptionStoryLikeRepository likeRepository;
    private final StorageService storageService;

    public AdoptionStoryService(
            AdoptionStoryRepository storyRepository,
            AdoptionStoryLikeRepository likeRepository,
            StorageService storageService
    ) {
        this.storyRepository = storyRepository;
        this.likeRepository = likeRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Page<AdoptionStoryResponse> getStories(Pageable pageable, User currentUser) {
        Long userId = currentUser == null ? null : currentUser.getId();
        return storyRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(story -> AdoptionStoryResponse.from(story, isLiked(story.getId(), userId)));
    }

    @Transactional(readOnly = true)
    public AdoptionStoryResponse getStory(Long id, User currentUser) {
        AdoptionStory story = findStory(id);
        Long userId = currentUser == null ? null : currentUser.getId();
        return AdoptionStoryResponse.from(story, isLiked(story.getId(), userId));
    }

    @Transactional
    public AdoptionStoryResponse createStory(User user, String title, String content, String animalNo, MultipartFile image) {
        validate(title, content);
        String imageUrl = saveImage(image);
        AdoptionStory story = storyRepository.save(new AdoptionStory(
                user.getId(),
                user.getName(),
                StringUtils.hasText(animalNo) ? animalNo.trim() : null,
                title.trim(),
                content.trim(),
                imageUrl
        ));
        return AdoptionStoryResponse.from(story, false);
    }

    @Transactional
    public void deleteStory(Long id, User user) {
        AdoptionStory story = findStory(id);
        if (!story.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can delete only your own story.");
        }
        likeRepository.deleteAllByStoryId(id);
        storyRepository.delete(story);
        storageService.delete(story.getImageUrl());
    }

    @Transactional
    public Map<String, Object> toggleLike(Long id, User user) {
        AdoptionStory story = findStory(id);
        return likeRepository.findByStoryIdAndUserId(id, user.getId())
                .map(like -> {
                    likeRepository.delete(like);
                    story.decreaseLikeCount();
                    return Map.<String, Object>of("liked", false, "likeCount", story.getLikeCount());
                })
                .orElseGet(() -> {
                    likeRepository.save(new AdoptionStoryLike(id, user.getId()));
                    story.increaseLikeCount();
                    return Map.<String, Object>of("liked", true, "likeCount", story.getLikeCount());
                });
    }

    private AdoptionStory findStory(Long id) {
        return storyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found."));
    }

    private boolean isLiked(Long storyId, Long userId) {
        return userId != null && likeRepository.existsByStoryIdAndUserId(storyId, userId);
    }

    private void validate(String title, String content) {
        if (!StringUtils.hasText(title) || title.trim().length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required and must be under 160 characters.");
        }
        if (!StringUtils.hasText(content) || content.trim().length() < 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must be at least 50 characters.");
        }
    }

    private String saveImage(MultipartFile image) {
        return storageService.store(image, "stories");
    }
}
