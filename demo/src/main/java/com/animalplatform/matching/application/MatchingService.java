package com.animalplatform.matching.application;

import com.animalplatform.external.publicapi.PublicAnimalApiClient;
import com.animalplatform.matching.presentation.AnimalMatchResult;
import com.animalplatform.matching.presentation.MatchingRequest;
import com.animalplatform.matching.presentation.MatchingRequest.ActivityLevel;
import com.animalplatform.matching.presentation.MatchingRequest.HousingType;
import com.animalplatform.matching.presentation.MatchingRequest.PreferredSize;
import com.animalplatform.matching.presentation.MatchingRequest.PreferredSpecies;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {

    private static final int SNAPSHOT_MAX_PAGES = 5;
    private static final int SNAPSHOT_PAGE_SIZE = 100;
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Set<String> LARGE_DOG_KEYWORDS = Set.of(
            "리트리버", "골든", "래브라도", "허스키", "말라뮤트", "셰퍼드", "도베르만",
            "로트와일러", "그레이트", "마스티프", "버니즈", "진돗개", "풍산", "아키타"
    );
    private static final Set<String> AGGRESSIVE_KEYWORDS = Set.of(
            "공격", "입질", "물림", "사나움", "사나운", "경계", "으르렁", "짖음 심함"
    );

    private final PublicAnimalApiClient publicAnimalApiClient;

    public MatchingService(PublicAnimalApiClient publicAnimalApiClient) {
        this.publicAnimalApiClient = publicAnimalApiClient;
    }

    public List<AnimalMatchResult> findMatches(MatchingRequest request) {
        return publicAnimalApiClient.fetchAbandonedAnimalsSnapshot(SNAPSHOT_MAX_PAGES, SNAPSHOT_PAGE_SIZE)
                .stream()
                .map(item -> score(item, request))
                .sorted(Comparator.comparingInt(AnimalMatchResult::matchScore).reversed())
                .limit(5)
                .toList();
    }

    private AnimalMatchResult score(Map<String, Object> item, MatchingRequest request) {
        int score = 70;
        List<String> reasons = new ArrayList<>();
        Species species = detectSpecies(item);
        Size size = detectSize(item);
        String kind = firstText(item, "kindNm", "kindFullNm", "kindCd");
        String specialMark = firstText(item, "specialMark");

        if (request.housingType() == HousingType.APARTMENT && species == Species.DOG && isLargeDog(kind, item)) {
            score -= 30;
            reasons.add("아파트 생활에는 대형견 활동 공간이 부족할 수 있어요.");
        }

        if (request.activityLevel() == ActivityLevel.HIGH && species == Species.DOG) {
            score += 20;
            reasons.add("활동량이 높은 보호자와 산책을 좋아하는 강아지는 잘 맞아요.");
        }

        if (request.hasChildren() && containsAny(specialMark, AGGRESSIVE_KEYWORDS)) {
            score -= 40;
            reasons.add("어린이가 있는 가정이라 특이사항의 공격성 키워드를 주의했어요.");
        }

        if (request.workHoursPerDay() > 8 && species == Species.CAT) {
            score += 15;
            reasons.add("근무 시간이 긴 편이라 독립적인 고양이와 잘 맞을 수 있어요.");
        }

        if (request.preferredSpecies() != PreferredSpecies.ANY) {
            if (matchesPreferredSpecies(request.preferredSpecies(), species)) {
                score += 12;
                reasons.add("선호하는 동물 종류와 일치해요.");
            } else {
                score -= 18;
                reasons.add("선호 동물 종류와 달라 우선순위를 낮췄어요.");
            }
        }

        if (request.preferredSize() != PreferredSize.ANY) {
            if (matchesPreferredSize(request.preferredSize(), size)) {
                score += 10;
                reasons.add("선호하는 체구와 잘 맞아요.");
            } else {
                score -= 12;
                reasons.add("선호 체구와 조금 달라요.");
            }
        }

        if (request.hasOtherPets()) {
            if (containsAny(specialMark, Set.of("사회성", "온순", "순함", "사람 좋아", "친화"))) {
                score += 8;
                reasons.add("다른 반려동물과 지내기 좋은 성향 단서가 있어요.");
            } else if (containsAny(specialMark, Set.of("단독", "분리", "경계", "입질"))) {
                score -= 10;
                reasons.add("다른 반려동물과의 합사는 천천히 확인하는 편이 좋아요.");
            }
        }

        if (reasons.isEmpty()) {
            reasons.add("입력한 생활 조건과 무난하게 맞는 후보예요.");
        }

        return toResult(item, clamp(score), reasons);
    }

    private AnimalMatchResult toResult(Map<String, Object> item, int score, List<String> reasons) {
        String shelterCode = firstText(item, "careRegNo");
        String desertionNo = firstText(item, "desertionNo");
        return new AnimalMatchResult(
                parseLong(desertionNo),
                desertionNo,
                firstText(item, "noticeNo", "desertionNo"),
                firstText(item, "kindNm", "kindFullNm", "upKindNm", "kindCd", "desertionNo"),
                firstText(item, "upKindNm", "upKindCd"),
                normalizeSex(firstText(item, "sexCd")),
                firstText(item, "age"),
                firstText(item, "weight"),
                firstText(item, "orgNm", "careAddr"),
                normalizeStatus(firstText(item, "processState")),
                firstImage(item),
                imageUrls(item),
                shelterIdFromCode(shelterCode),
                firstText(item, "careNm"),
                firstText(item, "specialMark"),
                score,
                List.copyOf(reasons)
        );
    }

    private boolean matchesPreferredSpecies(PreferredSpecies preferredSpecies, Species species) {
        return (preferredSpecies == PreferredSpecies.DOG && species == Species.DOG)
                || (preferredSpecies == PreferredSpecies.CAT && species == Species.CAT);
    }

    private boolean matchesPreferredSize(PreferredSize preferredSize, Size size) {
        return (preferredSize == PreferredSize.SMALL && size == Size.SMALL)
                || (preferredSize == PreferredSize.MEDIUM && size == Size.MEDIUM)
                || (preferredSize == PreferredSize.LARGE && size == Size.LARGE);
    }

    private boolean isLargeDog(String kind, Map<String, Object> item) {
        return detectSize(item) == Size.LARGE || containsAny(kind, LARGE_DOG_KEYWORDS);
    }

    private Species detectSpecies(Map<String, Object> item) {
        String source = firstText(item, "upKindNm", "upKindCd", "kindNm", "kindCd");
        if (source.contains("고양") || source.equalsIgnoreCase("CAT") || source.contains("422400")) {
            return Species.CAT;
        }
        if (source.contains("개") || source.equalsIgnoreCase("DOG") || source.contains("417000")) {
            return Species.DOG;
        }
        return Species.OTHER;
    }

    private Size detectSize(Map<String, Object> item) {
        double weight = extractWeight(firstText(item, "weight"));
        if (weight > 0) {
            if (weight <= 8) return Size.SMALL;
            if (weight <= 20) return Size.MEDIUM;
            return Size.LARGE;
        }
        String kind = firstText(item, "kindNm", "kindFullNm", "kindCd");
        if (containsAny(kind, LARGE_DOG_KEYWORDS)) {
            return Size.LARGE;
        }
        return Size.MEDIUM;
    }

    private double extractWeight(String weightText) {
        if (weightText == null) {
            return -1;
        }
        Matcher matcher = WEIGHT_PATTERN.matcher(weightText);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : -1;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private boolean containsAny(String value, Set<String> keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return keywords.stream().anyMatch(value::contains);
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "PROTECTING";
        }
        if (containsAny(value, Set.of("보호", "공고"))) {
            return "PROTECTING";
        }
        if (containsAny(value, Set.of("상담", "예약", "진행"))) {
            return "IN_COUNSELING";
        }
        if (containsAny(value, Set.of("종료", "입양", "반환", "자연사", "안락사"))) {
            return "COMPLETED";
        }
        return "OTHER";
    }

    private String normalizeSex(String value) {
        if (value == null || value.isBlank()) {
            return "정보없음";
        }
        return switch (value.trim().toUpperCase()) {
            case "F" -> "암컷";
            case "M" -> "수컷";
            case "Q" -> "미상";
            default -> value;
        };
    }

    private String firstText(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                    return text;
                }
            }
        }
        return "";
    }

    private String firstImage(Map<String, Object> source) {
        return imageUrls(source).stream().findFirst().orElse("");
    }

    private List<String> imageUrls(Map<String, Object> source) {
        return Arrays.stream(new String[]{"popfile1", "popfile2", "popfile3", "popfile4", "popfile5", "popfile"})
                .map(key -> firstText(source, key))
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private Long shelterIdFromCode(String shelterCode) {
        if (shelterCode == null || shelterCode.isBlank()) {
            return 0L;
        }
        return (long) Math.abs(shelterCode.hashCode());
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception exception) {
            return Math.abs((long) String.valueOf(value).hashCode());
        }
    }

    private enum Species {
        DOG,
        CAT,
        OTHER
    }

    private enum Size {
        SMALL,
        MEDIUM,
        LARGE
    }
}
