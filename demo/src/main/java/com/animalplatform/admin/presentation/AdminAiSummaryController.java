package com.animalplatform.admin.presentation;

import com.animalplatform.animal.domain.AnimalAiSummary;
import com.animalplatform.animal.domain.AnimalAiSummaryRepository;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자", description = "관리자 AI 소개 관리 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/admin/ai-summary")
public class AdminAiSummaryController {

    private static final Pattern BROKEN_SUMMARY_PATTERN = Pattern.compile(
            ".*(Menschen|inmediatamente|[a-zA-Z]{10,}|[\\u4E00-\\u9FFF\\u3040-\\u30FF]).*",
            Pattern.DOTALL
    );

    private final AnimalAiSummaryRepository animalAiSummaryRepository;

    public AdminAiSummaryController(AnimalAiSummaryRepository animalAiSummaryRepository) {
        this.animalAiSummaryRepository = animalAiSummaryRepository;
    }

    @DeleteMapping("/clear-broken")
    @Transactional
    @Operation(summary = "깨진 AI 소개 삭제", description = "외국어가 섞였거나 깨진 AI 소개 캐시를 삭제합니다.")
    public ApiResponse<Map<String, Object>> clearBrokenSummaries() {
        List<AnimalAiSummary> brokenSummaries = animalAiSummaryRepository.findAll().stream()
                .filter(summary -> isBroken(summary.getSummaryText()))
                .toList();

        animalAiSummaryRepository.deleteAllInBatch(brokenSummaries);

        return ApiResponse.ok(Map.of(
                "deletedCount", brokenSummaries.size()
        ));
    }

    private boolean isBroken(String summaryText) {
        return StringUtils.hasText(summaryText) && BROKEN_SUMMARY_PATTERN.matcher(summaryText).matches();
    }
}
