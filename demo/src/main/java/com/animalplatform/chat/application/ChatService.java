package com.animalplatform.chat.application;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.animalplatform.animal.application.OpenRouterClient;
import com.animalplatform.chat.domain.ChatQuickAnswerRepository;
import com.animalplatform.chat.presentation.ChatController.ChatMessageRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private static final String KOREAN_ONLY_PREFIX = "(한국어로만 답변해주세요) ";
    private static final String SYSTEM_PROMPT = String.join("\n",
            "반드시 한국어로만 답변하세요. Never use Chinese or Japanese characters.",
            "절대 중국어, 영어, 일본어로 답하지 마세요.",
            "당신은 유기동물 입양 상담사 '포동이'입니다.",
            "입양을 고려하는 사람들에게 친절하고 현실적인 조언을 제공하세요.",
            "거주환경, 생활패턴, 경험, 가족 구성을 파악하여 입양 준비도를 평가하세요.",
            "무책임한 입양을 부드럽게 만류하고, 준비된 입양자에게는 적합한 동물 유형을 추천하세요.",
            "답변은 3문장 이내로 간결하게, 이모지를 적절히 사용하세요."
    );

    private final OpenRouterClient openRouterClient;
    private final ChatQuickAnswerRepository quickAnswerRepository;

    public ChatService(OpenRouterClient openRouterClient, ChatQuickAnswerRepository quickAnswerRepository) {
        this.openRouterClient = openRouterClient;
        this.quickAnswerRepository = quickAnswerRepository;
    }

    public String reply(List<ChatMessageRequest> messages) {
        String latestUserMessage = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .filter(message -> StringUtils.hasText(message.content()))
                .reduce((first, second) -> second)
                .map(message -> trimContent(message.content()))
                .orElse("");
        if (StringUtils.hasText(latestUserMessage)) {
            var quickAnswer = quickAnswerRepository.findByTriggerKeyword(latestUserMessage.trim())
                    .filter(com.animalplatform.chat.domain.ChatQuickAnswer::isActive);
            if (quickAnswer.isPresent()) {
                return quickAnswer.get().getAnswerText();
            }
        }

        List<Map<String, String>> filteredMessages = messages.stream()
                .filter(message -> isAllowedRole(message.role()))
                .filter(message -> StringUtils.hasText(message.content()))
                .map(message -> Map.of(
                        "role", message.role(),
                        "content", normalizeContent(message.role(), message.content())
                ))
                .toList();

        if (filteredMessages.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one message is required.");
        }

        if ("assistant".equals(filteredMessages.getFirst().get("role"))) {
            throw new ResponseStatusException(BAD_REQUEST, "Conversation must start with a user message");
        }

        // The frontend greeting is UI-only, but this also normalizes bypassed clients before calling OpenRouter.
        List<Map<String, String>> openRouterMessages = collapseConsecutiveSameRoleMessages(filteredMessages);
        return openRouterClient.callChat(SYSTEM_PROMPT, openRouterMessages, 1_000);
    }

    private boolean isAllowedRole(String role) {
        return "user".equals(role) || "assistant".equals(role);
    }

    private String trimContent(String content) {
        String trimmed = content.trim();
        return trimmed.length() <= 1_000 ? trimmed : trimmed.substring(0, 1_000);
    }

    private String normalizeContent(String role, String content) {
        String trimmed = trimContent(content);
        if (!"user".equals(role)) {
            return trimmed;
        }
        return trimmed.startsWith(KOREAN_ONLY_PREFIX) ? trimmed : KOREAN_ONLY_PREFIX + trimmed;
    }

    private List<Map<String, String>> collapseConsecutiveSameRoleMessages(List<Map<String, String>> messages) {
        List<Map<String, String>> normalized = new ArrayList<>();
        for (Map<String, String> message : messages) {
            if (!normalized.isEmpty()
                    && normalized.getLast().get("role").equals(message.get("role"))) {
                normalized.set(normalized.size() - 1, message);
                continue;
            }
            normalized.add(message);
        }
        return normalized;
    }
}
