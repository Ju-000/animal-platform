package com.animalplatform.auth.presentation;


import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.auth.application.LocalAuthService;
import com.animalplatform.auth.config.SocialLoginProperties;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "인증", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SocialLoginProperties socialLoginProperties;
    private final LocalAuthService localAuthService;
    public AuthController(SocialLoginProperties socialLoginProperties, LocalAuthService localAuthService) {
        this.socialLoginProperties = socialLoginProperties;
        this.localAuthService = localAuthService;
    }
    @GetMapping("/social/providers")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<List<Map<String, Object>>> getSocialProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        addProvider(providers, "google", "Google", socialLoginProperties.getGoogle());
        addProvider(providers, "kakao", "Kakao", socialLoginProperties.getKakao());
        addProvider(providers, "naver", "Naver", socialLoginProperties.getNaver());
        return ApiResponse.ok(providers);
    }
    @GetMapping("/check-username")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> checkUsername(@RequestParam String username) {
        boolean available = localAuthService.isUsernameAvailable(username);
        return ApiResponse.ok(Map.of("available", available));
    }

    @GetMapping("/check-nickname")
        @Operation(summary = "닉네임 중복 확인", description = "회원가입 전에 사용할 수 있는 닉네임인지 확인합니다.")
    public ApiResponse<Map<String, Object>> checkNickname(@RequestParam String nickname) {
        boolean available = localAuthService.isNicknameAvailable(nickname);
        return ApiResponse.ok(Map.of("available", available));
    }
    @PostMapping("/signup")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> signUp(@Valid @RequestBody SignUpRequest request) {
        User user = localAuthService.signUp(
                request.username(),
                request.password(),
                request.name(),
                request.phone(),
                request.email(),
                request.birthDate(),
                request.gender(),
                request.address(),
                Boolean.TRUE.equals(request.privacyConsent())
        );
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "role", user.getRole().name()
        ), "Sign-up completed.");
    }
    @PostMapping("/login")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        User user = localAuthService.login(request.username(), request.password(), httpRequest, httpResponse);
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "name", user.getName(),
                "phone", user.getPhone(),
                "role", user.getRole().name()
        ), "Login completed.");
    }
    @PostMapping("/logout")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        localAuthService.logout(request, response);
        return ApiResponse.ok(null, "Logged out.");
    }
    public record SignUpRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String name,
            @NotBlank String phone,
            @NotBlank String email,
            String birthDate,
            String gender,
            String address,
            Boolean privacyConsent
    ) {
    }
    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }
    private void addProvider(List<Map<String, Object>> providers, String id, String name,
                             SocialLoginProperties.Provider provider) {
        boolean enabled = StringUtils.hasText(provider.getClientId()) && StringUtils.hasText(provider.getClientSecret());
        providers.add(Map.of(
                "id", id,
                "name", name,
                "enabled", enabled,
                "authorizationUrl", "/oauth2/authorization/" + id
        ));
    }
}
