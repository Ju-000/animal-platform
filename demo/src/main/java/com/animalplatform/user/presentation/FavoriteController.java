package com.animalplatform.user.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.auth.application.AuthenticatedUserService;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.favorite.application.FavoriteService;
import com.animalplatform.user.domain.User;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "관심동물", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final AuthenticatedUserService authenticatedUserService;
    private final FavoriteService favoriteService;

    public FavoriteController(AuthenticatedUserService authenticatedUserService, FavoriteService favoriteService) {
        this.authenticatedUserService = authenticatedUserService;
        this.favoriteService = favoriteService;
    }

    @GetMapping
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<List<String>> getFavorites() {
        User user = authenticatedUserService.getCurrentUser();
        return ApiResponse.ok(favoriteService.getFavoriteAnimalNos(user.getId()));
    }

    @PostMapping("/{animalNo}")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> addFavorite(@PathVariable String animalNo) {
        User user = authenticatedUserService.getCurrentUser();
        boolean favorited = favoriteService.addFavorite(user.getId(), animalNo);
        return ApiResponse.ok(Map.of(
                "animalNo", animalNo,
                "favorited", favorited
        ));
    }

    @DeleteMapping("/{animalNo}")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> removeFavorite(@PathVariable String animalNo) {
        User user = authenticatedUserService.getCurrentUser();
        boolean favorited = favoriteService.removeFavorite(user.getId(), animalNo);
        return ApiResponse.ok(Map.of(
                "animalNo", animalNo,
                "favorited", favorited
        ));
    }
}
