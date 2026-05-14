package com.animalplatform.auth.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.common.api.ApiResponse;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "인증", description = "해당 도메인 API")
@CommonApiResponses
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ApiResponse.ok(Map.of(
                "parameterName", csrfToken.getParameterName(),
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        ));
    }
}
