package com.animalplatform.chat.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import com.animalplatform.animal.application.OpenRouterApiException;
import com.animalplatform.chat.application.ChatQuickAnswerService;
import com.animalplatform.chat.application.ChatService;
import com.animalplatform.common.api.ApiResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
@Tag(name = "AI", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final String SESSION_COUNT_KEY = "podongChatMessageCount";
    private static final int MAX_MESSAGES_PER_SESSION = 20;

    private final ChatService chatService;
    private final ChatQuickAnswerService quickAnswerService;

    public ChatController(ChatService chatService, ChatQuickAnswerService quickAnswerService) {
        this.chatService = chatService;
        this.quickAnswerService = quickAnswerService;
    }

    @GetMapping("/quick-answers")
    @Operation(summary = "빠른 답변 버튼 목록 조회", description = "AI 호출 없이 사용할 빠른 질문 버튼 목록을 반환합니다.")
    public ApiResponse<List<Map<String, String>>> getQuickAnswers() {
        return ApiResponse.ok(quickAnswerService.getQuickAnswerButtons());
    }

    @PostMapping("/quick-answer")
    @Operation(summary = "빠른 답변 조회", description = "키워드와 매칭되는 답변을 DB에서 즉시 반환합니다.")
    public ApiResponse<Map<String, String>> getQuickAnswer(@Valid @RequestBody QuickAnswerRequest request) {
        return ApiResponse.ok(quickAnswerService.getQuickAnswer(request.keyword()));
    }

    @PostMapping
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ResponseEntity<?> chat(
            @Valid @RequestBody ChatRequest request,
            HttpSession session
    ) {
        int currentCount = session.getAttribute(SESSION_COUNT_KEY) instanceof Integer count ? count : 0;
        if (currentCount >= MAX_MESSAGES_PER_SESSION) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Chat message limit exceeded for this session.");
        }

        String response;
        try {
            response = chatService.reply(request.messages());
        } catch (OpenRouterApiException exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "포동이가 잠시 자리를 비웠어요. 잠시 후 다시 말을 걸어주세요 🐾"));
        }
        session.setAttribute(SESSION_COUNT_KEY, currentCount + 1);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "role", "assistant",
                "content", response,
                "remainingMessages", Math.max(0, MAX_MESSAGES_PER_SESSION - currentCount - 1)
        )));
    }

    public record ChatRequest(
            @NotEmpty
            @Size(max = 20)
            List<@Valid ChatMessageRequest> messages
    ) {
    }

    public record ChatMessageRequest(
            @NotBlank
            String role,
            @NotBlank
            @Size(max = 1000)
            String content
    ) {
    }
}
