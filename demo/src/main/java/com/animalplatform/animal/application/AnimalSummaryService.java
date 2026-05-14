package com.animalplatform.animal.application;

import com.animalplatform.animal.domain.AnimalAiSummary;
import com.animalplatform.animal.domain.AnimalAiSummaryRepository;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AnimalSummaryService {

    private static final String FALLBACK_SUMMARY = "AI 소개를 준비 중이에요. 잠시 후 다시 확인해주세요 🐾";

    private static final String SYSTEM_PROMPT = String.join("\n",
            "IMPORTANT: Respond in Korean (한국어) ONLY.",
            "No English, No Chinese, No Spanish, No German, No Japanese.",
            "반드시 순수한 한국어로만 답변하세요.",
            "당신은 유기동물 입양 플랫폼의 동물 소개 전문가입니다.",
            "주어진 동물 정보를 바탕으로 따뜻하고 친근한 톤으로 2~3문장의 소개글을 작성하세요.",
            "마지막 줄에 '입양 적합 환경: '으로 시작하는 추천을 한 줄로 추가하세요.",
            "",
            "주거 환경 추천 기준:",
            "- 대형견(25kg 이상) 또는 진돗개/허스키/리트리버 등 활동량 높은 견종: 마당 있는 단독주택 필수",
            "- 중형견(10~25kg): 빌라 또는 연립주택 이상 권장",
            "- 소형견(10kg 미만) 또는 고양이: 아파트, 빌라, 오피스텔 모두 적합",
            "- 노령동물(7세 이상): 조용한 단독주택 또는 빌라 권장",
            "- 특이사항에 예민, 공격성, 겁이 많음 같은 키워드가 있으면 조용한 주거 환경 우선 추천"
    );

    private final OpenRouterClient openRouterClient;
    private final AnimalAiSummaryRepository animalAiSummaryRepository;

    public AnimalSummaryService(OpenRouterClient openRouterClient, AnimalAiSummaryRepository animalAiSummaryRepository) {
        this.openRouterClient = openRouterClient;
        this.animalAiSummaryRepository = animalAiSummaryRepository;
    }

    @Transactional
    public String generateSummary(AnimalDetail animal) {
        LocalDateTime now = LocalDateTime.now();

        return animalAiSummaryRepository.findByDesertionNo(animal.desertionNo())
                .filter(summary -> summary.getExpiresAt().isAfter(now))
                .filter(summary -> isUsableSummary(summary.getSummaryText()))
                .map(AnimalAiSummary::getSummaryText)
                .orElseGet(() -> generateAndPersistSummary(animal, now));
    }

    private String generateAndPersistSummary(AnimalDetail animal, LocalDateTime now) {
        String prompt = buildPrompt(animal);
        String summaryText = openRouterClient.callSummary(SYSTEM_PROMPT, prompt, 1_000);
        if (hasMixedForeignText(summaryText)) {
            summaryText = openRouterClient.callSummary(SYSTEM_PROMPT, prompt, 1_000);
        }
        if (!isUsableSummary(summaryText)) {
            return FALLBACK_SUMMARY;
        }

        LocalDateTime expiresAt = now.plusDays(30);
        String finalSummaryText = summaryText;
        AnimalAiSummary summary = animalAiSummaryRepository.findByDesertionNo(animal.desertionNo())
                .orElseGet(() -> new AnimalAiSummary(animal.desertionNo(), finalSummaryText, now, expiresAt));
        summary.refresh(finalSummaryText, now, expiresAt);
        animalAiSummaryRepository.save(summary);

        return finalSummaryText;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void deleteExpiredSummaries() {
        animalAiSummaryRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }

    private String buildPrompt(AnimalDetail animal) {
        return """
                (한국어로만 답변) 아래 유기동물 정보를 바탕으로 입양자가 이해하기 쉬운 소개글을 작성해주세요.

                desertionNo: %s
                kindCd(품종): %s
                colorCd(색상): %s
                age(나이): %s
                weight(체중): %s
                sexCd(성별): %s
                neuterYn(중성화): %s
                specialMark(특이사항): %s
                processState(상태): %s
                """.formatted(
                fallback(animal.desertionNo()),
                fallback(animal.kindCd()),
                fallback(animal.colorCd()),
                fallback(animal.age()),
                fallback(animal.weight()),
                fallback(animal.sexCd()),
                fallback(animal.neuterYn()),
                fallback(animal.specialMark()),
                fallback(animal.processState())
        );
    }

    private String fallback(String value) {
        return StringUtils.hasText(value) ? value : "정보 없음";
    }

    private boolean isUsableSummary(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return !"null".equalsIgnoreCase(value.trim()) && !hasMixedForeignText(value);
    }

    private boolean hasMixedForeignText(String value) {
        if (!StringUtils.hasText(value) || value.length() <= 50) {
            return false;
        }
        return value.matches(".*[a-zA-Z\\u4E00-\\u9FFF\\u3040-\\u30FF]+.*");
    }
}
