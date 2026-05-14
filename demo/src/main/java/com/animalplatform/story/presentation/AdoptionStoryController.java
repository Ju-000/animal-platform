package com.animalplatform.story.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.auth.application.AuthenticatedUserService;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.story.application.AdoptionStoryService;
import com.animalplatform.user.domain.User;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
@Tag(name = "후기", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/stories")
public class AdoptionStoryController {

    private final AdoptionStoryService storyService;
    private final AuthenticatedUserService authenticatedUserService;

    public AdoptionStoryController(AdoptionStoryService storyService, AuthenticatedUserService authenticatedUserService) {
        this.storyService = storyService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Page<AdoptionStoryResponse>> getStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 30));
        User currentUser = authenticatedUserService.getCurrentUserOptional().orElse(null);
        return ApiResponse.ok(storyService.getStories(pageable, currentUser));
    }

    @GetMapping("/{id}")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<AdoptionStoryResponse> getStory(@PathVariable Long id) {
        User currentUser = authenticatedUserService.getCurrentUserOptional().orElse(null);
        return ApiResponse.ok(storyService.getStory(id, currentUser));
    }

    @PostMapping
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<AdoptionStoryResponse> createStory(
            @RequestPart String title,
            @RequestPart String content,
            @RequestPart(required = false) String animalNo,
            @RequestPart(required = false) MultipartFile image
    ) {
        User user = authenticatedUserService.getCurrentUser();
        return ApiResponse.ok(storyService.createStory(user, title, content, animalNo, image));
    }

    @DeleteMapping("/{id}")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Void> deleteStory(@PathVariable Long id) {
        User user = authenticatedUserService.getCurrentUser();
        storyService.deleteStory(id, user);
        return ApiResponse.ok(null, "Story deleted.");
    }

    @PostMapping("/{id}/like")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long id) {
        User user = authenticatedUserService.getCurrentUser();
        return ApiResponse.ok(storyService.toggleLike(id, user));
    }
}
