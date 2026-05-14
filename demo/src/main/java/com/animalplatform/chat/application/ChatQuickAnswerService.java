package com.animalplatform.chat.application;

import com.animalplatform.chat.domain.ChatQuickAnswer;
import com.animalplatform.chat.domain.ChatQuickAnswerRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatQuickAnswerService {

    private final ChatQuickAnswerRepository repository;

    public ChatQuickAnswerService(ChatQuickAnswerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getQuickAnswerButtons() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(answer -> Map.of(
                        "label", answer.getButtonLabel(),
                        "keyword", answer.getTriggerKeyword()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, String> getQuickAnswer(String keyword) {
        ChatQuickAnswer answer = repository.findByTriggerKeyword(keyword.trim())
                .filter(ChatQuickAnswer::isActive)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "빠른 답변을 찾을 수 없습니다."
                ));
        return Map.of(
                "role", "assistant",
                "content", answer.getAnswerText()
        );
    }
}
