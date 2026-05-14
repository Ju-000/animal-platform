package com.animalplatform.animal.presentation;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.animalplatform.common.api.CommonApiResponses;
import com.animalplatform.animal.application.AnimalDetail;
import com.animalplatform.animal.application.OpenRouterApiException;
import com.animalplatform.animal.application.AnimalService;
import com.animalplatform.animal.application.AnimalSummaryService;
import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.external.publicapi.PublicAnimalApiClient;
import com.animalplatform.external.publicapi.PublicAnimalApiClient.PublicApiPage;
import com.animalplatform.statistics.domain.AnimalSnapshot;
import com.animalplatform.statistics.domain.SnapshotRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "동물", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/animals")
public class AnimalController {

    private static final Logger log = LoggerFactory.getLogger(AnimalController.class);
    private static final String STATUS_PROTECTING = "PROTECTING";
    private static final String STATUS_IN_COUNSELING = "IN_COUNSELING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_OTHER = "OTHER";
    private static final String AI_SUMMARY_FALLBACK = "✨ AI 소개를 준비 중이에요. 잠시 후 다시 확인해주세요 🐾";

    private static final List<String> METRO_REGIONS = List.of(
            "서울특별시",
            "부산광역시",
            "대구광역시",
            "인천광역시",
            "광주광역시",
            "대전광역시",
            "울산광역시",
            "세종특별자치시",
            "경기도",
            "강원특별자치도",
            "충청북도",
            "충청남도",
            "전북특별자치도",
            "전라남도",
            "경상북도",
            "경상남도",
            "제주특별자치도"
    );

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final PublicAnimalApiClient publicAnimalApiClient;
    private final AnimalService animalService;
    private final AnimalSummaryService animalSummaryService;
    private final SnapshotRepository snapshotRepository;

    public AnimalController(
            PublicAnimalApiClient publicAnimalApiClient,
            AnimalService animalService,
            AnimalSummaryService animalSummaryService,
            SnapshotRepository snapshotRepository
    ) {
        this.publicAnimalApiClient = publicAnimalApiClient;
        this.animalService = animalService;
        this.animalSummaryService = animalSummaryService;
        this.snapshotRepository = snapshotRepository;
    }

    @GetMapping("/filters")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getAnimalFilters() {
        List<Map<String, Object>> snapshot = publicAnimalApiClient.fetchAbandonedAnimalsSnapshot(10, 100);

        List<String> speciesOptions = snapshot.stream()
                .map(item -> firstText(item, "upKindNm", "upKindCd"))
                .filter(this::hasText)
                .distinct()
                .sorted()
                .toList();

        List<Map<String, String>> statusOptions = snapshot.stream()
                .map(item -> normalizeStatus(firstText(item, "processState")))
                .filter(this::hasText)
                .distinct()
                .map(this::toStatusOption)
                .toList();

        return ApiResponse.ok(Map.of(
                "regions", METRO_REGIONS,
                "species", speciesOptions,
                "statuses", statusOptions
        ));
    }

    @GetMapping
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getAnimals(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String orgNm,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sex,
            @RequestParam(required = false) String neutered,
            @RequestParam(required = false) Boolean protectingOnly,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));

        if (snapshotRepository.count() == 0) {
            log.warn("AnimalSnapshot empty, falling back to public API");
            String regionFilter = hasText(orgNm) ? orgNm : region;
            return getAnimalsFromPublicApi(regionFilter, species, breed, status, sex, neutered, protectingOnly, startDate, endDate, safePage, safeSize);
        }

        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String processState = Boolean.TRUE.equals(protectingOnly) && !hasText(status) ? STATUS_PROTECTING : status;
        String kindFilter = hasText(breed) ? breed : species;
        String regionFilter = hasText(orgNm) ? orgNm : region;

        Page<AnimalSnapshot> snapshotPage = snapshotRepository.findByFilters(
                kindFilter,
                regionFilter,
                processState,
                sex,
                neutered,
                start,
                end,
                PageRequest.of(
                        safePage - 1,
                        safeSize,
                        Sort.by(Sort.Order.desc("happenDt"), Sort.Order.desc("desertionNo"))
                )
        );

        List<Map<String, Object>> content = snapshotPage.getContent().stream()
                .map(animalService::toPublicApiShape)
                .map(this::toAnimalSummary)
                .toList();

        return ApiResponse.ok(Map.of(
                "content", content,
                "page", snapshotPage.getNumber(),
                "size", snapshotPage.getSize(),
                "totalElements", snapshotPage.getTotalElements(),
                "totalPages", Math.max(snapshotPage.getTotalPages(), 1)
        ));
    }

    private ApiResponse<Map<String, Object>> getAnimalsFromPublicApi(
            String region,
            String species,
            String breed,
            String status,
            String sex,
            String neutered,
            Boolean protectingOnly,
            String startDate,
            String endDate,
            int page,
            int size
    ) {
        boolean hasDetailedFilters = hasText(region)
                || hasText(species)
                || hasText(breed)
                || hasText(status)
                || hasText(sex)
                || hasText(neutered)
                || Boolean.TRUE.equals(protectingOnly)
                || hasText(startDate)
                || hasText(endDate);

        if (!hasDetailedFilters) {
            PublicApiPage resultPage = publicAnimalApiClient.fetchAbandonedAnimalPage(page, size);
            List<Map<String, Object>> content = resultPage.items().stream()
                    .map(this::toAnimalSummary)
                    .toList();

            int totalPages = (int) Math.ceil((double) resultPage.totalCount() / size);

            return ApiResponse.ok(Map.of(
                    "content", content,
                    "page", page - 1,
                    "size", size,
                    "totalElements", resultPage.totalCount(),
                    "totalPages", Math.max(totalPages, 1)
            ));
        }

        List<Map<String, Object>> snapshot = fetchAllAnimals();
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);

        List<Map<String, Object>> filtered = snapshot.stream()
                .filter(item -> matchesFilters(item, region, species, breed, status, sex, neutered, protectingOnly, start, end))
                .map(this::toAnimalSummary)
                .toList();

        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(filtered.size(), fromIndex + size);
        List<Map<String, Object>> pageContent = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) filtered.size() / size);

        return ApiResponse.ok(Map.of(
                "content", pageContent,
                "page", page - 1,
                "size", size,
                "totalElements", filtered.size(),
                "totalPages", Math.max(totalPages, 1)
        ));
    }

    @GetMapping("/recommended")
        @Operation(summary = "추천 입양 동물 조회", description = "마감 임박, 장기 보호, 랜덤 순으로 오늘의 추천 입양 동물을 조회합니다.")
    public ApiResponse<List<Map<String, Object>>> getRecommendedAnimals(
            @RequestParam(required = false) String orgNm,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safeSize = Math.max(1, Math.min(size, 50));

        if (snapshotRepository.count() == 0) {
            log.warn("AnimalSnapshot empty, falling back to public API for recommendations");
            List<Map<String, Object>> content = publicAnimalApiClient.fetchAbandonedAnimalPage(1, safeSize)
                    .items()
                    .stream()
                    .map(this::toAnimalSummary)
                    .toList();
            return ApiResponse.ok(content);
        }

        List<Map<String, Object>> content = animalService.getRecommendedAnimals(orgNm, safeSize)
                .stream()
                .map(this::toAnimalSummary)
                .toList();

        return ApiResponse.ok(content);
    }

    @GetMapping("/{animalSlug}")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getAnimalDetail(@PathVariable String animalSlug) {
        String lookupToken = extractLookupToken(animalSlug);
        Map<String, Object> animal = toAnimalDetail(animalService.findAnimalByDesertionNo(lookupToken));

        return ApiResponse.ok(animal);
    }

    @GetMapping("/{desertionNo}/summary")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getAnimalSummary(@PathVariable String desertionNo) {
        String lookupToken = extractLookupToken(desertionNo);
        Map<String, Object> item = animalService.findAnimalByDesertionNo(lookupToken);

        AnimalDetail animalDetail = toAnimalAiDetail(item);
        String summary;
        try {
            summary = animalSummaryService.generateSummary(animalDetail);
        } catch (OpenRouterApiException exception) {
            log.warn("AI summary generation failed. desertionNo={} status={}",
                    animalDetail.desertionNo(),
                    exception.getStatusCode());
            summary = AI_SUMMARY_FALLBACK;
        }

        return ApiResponse.ok(Map.of(
                "desertionNo", animalDetail.desertionNo(),
                "summary", summary
        ));
    }

    private List<Map<String, Object>> fetchAllAnimals() {
        List<Map<String, Object>> all = new ArrayList<>();
        int page = 1;
        int size = 100;
        int totalCount = Integer.MAX_VALUE;

        while (all.size() < totalCount) {
            PublicApiPage resultPage = publicAnimalApiClient.fetchAbandonedAnimalPage(page, size);
            if (resultPage.items().isEmpty()) {
                break;
            }
            all.addAll(resultPage.items());
            totalCount = resultPage.totalCount();
            page++;
        }

        return List.copyOf(all);
    }

    private boolean matchesFilters(
            Map<String, Object> item,
            String region,
            String species,
            String breed,
            String status,
            String sex,
            String neutered,
            Boolean protectingOnly,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String regionText = normalizeMetroRegion(firstText(item, "orgNm", "careAddr"));
        String speciesText = firstText(item, "upKindNm", "upKindCd");
        String breedText = firstText(item, "kindNm", "kindFullNm", "kindCd");
        String statusText = normalizeStatus(firstText(item, "processState"));
        String sexText = normalizeSex(firstText(item, "sexCd"));
        String neuteredText = normalizeNeutered(firstText(item, "neuterYn"));
        LocalDate happenedAt = parseBasicDate(firstText(item, "happenDt"));

        boolean inDateRange = true;
        if (startDate != null && happenedAt != null) {
            inDateRange = !happenedAt.isBefore(startDate);
        }
        if (inDateRange && endDate != null && happenedAt != null) {
            inDateRange = !happenedAt.isAfter(endDate);
        }

        return (!hasText(region) || Objects.equals(regionText, region))
                && (!hasText(species) || Objects.equals(speciesText, species))
                && (!hasText(breed) || Objects.equals(breedText, breed))
                && (!hasText(status) || Objects.equals(statusText, status))
                && (!hasText(sex) || Objects.equals(sexText, sex))
                && (!hasText(neutered) || Objects.equals(neuteredText, neutered))
                && (!Boolean.TRUE.equals(protectingOnly) || Objects.equals(statusText, STATUS_PROTECTING))
                && inDateRange;
    }

    private Map<String, Object> toAnimalSummary(Map<String, Object> item) {
        String shelterCode = firstText(item, "careRegNo");
        String desertionNo = firstText(item, "desertionNo");

        return Map.ofEntries(
                Map.entry("id", parseLong(desertionNo)),
                Map.entry("desertionNo", desertionNo),
                Map.entry("name", firstText(item, "kindNm", "kindFullNm", "upKindNm", "kindCd", "desertionNo")),
                Map.entry("noticeNumber", firstText(item, "noticeNo", "desertionNo")),
                Map.entry("species", firstText(item, "upKindNm", "upKindCd")),
                Map.entry("sex", normalizeSex(firstText(item, "sexCd"))),
                Map.entry("ageText", firstText(item, "age")),
                Map.entry("weightText", firstText(item, "weight")),
                Map.entry("region", firstText(item, "orgNm", "careAddr")),
                Map.entry("serviceStatus", normalizeStatus(firstText(item, "processState"))),
                Map.entry("imageUrl", firstImage(item)),
                Map.entry("imageUrls", imageUrls(item)),
                Map.entry("shelterId", shelterIdFromCode(shelterCode)),
                Map.entry("shelterName", firstText(item, "careNm")),
                Map.entry("urgencyBadge", urgencyBadge(item))
        );
    }

    private Map<String, Object> toAnimalDetail(Map<String, Object> item) {
        String shelterCode = firstText(item, "careRegNo");
        String desertionNo = firstText(item, "desertionNo");

        return Map.ofEntries(
                Map.entry("id", parseLong(desertionNo)),
                Map.entry("desertionNo", desertionNo),
                Map.entry("name", firstText(item, "kindNm", "kindFullNm", "upKindNm", "kindCd", "desertionNo")),
                Map.entry("noticeNumber", firstText(item, "noticeNo", "desertionNo")),
                Map.entry("species", firstText(item, "upKindNm", "upKindCd")),
                Map.entry("sex", normalizeSex(firstText(item, "sexCd"))),
                Map.entry("ageText", firstText(item, "age")),
                Map.entry("weightText", firstText(item, "weight")),
                Map.entry("serviceStatus", normalizeStatus(firstText(item, "processState"))),
                Map.entry("foundPlace", firstText(item, "happenPlace")),
                Map.entry("specialMark", firstText(item, "specialMark")),
                Map.entry("summary", firstText(item, "specialMark")),
                Map.entry("imageUrl", firstImage(item)),
                Map.entry("imageUrls", imageUrls(item)),
                Map.entry("shelter", Map.of(
                        "id", shelterIdFromCode(shelterCode),
                        "name", firstText(item, "careNm"),
                        "address", firstText(item, "careAddr"),
                        "phone", firstText(item, "careTel")
                ))
        );
    }

    private boolean matchesAnimal(Map<String, Object> item, String lookupToken) {
        return Objects.equals(firstText(item, "noticeNo"), lookupToken)
                || Objects.equals(firstText(item, "desertionNo"), lookupToken);
    }

    private AnimalDetail toAnimalAiDetail(Map<String, Object> item) {
        return new AnimalDetail(
                firstText(item, "desertionNo", "noticeNo"),
                firstText(item, "kindCd", "kindNm", "kindFullNm"),
                firstText(item, "colorCd"),
                firstText(item, "age"),
                firstText(item, "weight"),
                firstText(item, "sexCd"),
                firstText(item, "neuterYn"),
                firstText(item, "specialMark"),
                firstText(item, "processState")
        );
    }

    private Map<String, String> toStatusOption(String status) {
        return Map.of(
                "value", status,
                "label", toStatusLabel(status)
        );
    }

    private String toStatusLabel(String status) {
        return switch (status) {
            case STATUS_PROTECTING -> "보호중";
            case STATUS_IN_COUNSELING -> "상담 진행 중";
            case STATUS_COMPLETED -> "완료(귀가)";
            default -> "기타";
        };
    }

    private String normalizeStatus(String value) {
        if (!hasText(value)) {
            return STATUS_PROTECTING;
        }

        if (containsAny(value,
                "보호중",
                "보호",
                "공고중",
                "공고")) {
            return STATUS_PROTECTING;
        }

        if (containsAny(value,
                "종료",
                "관리",
                "자연사",
                "안락사",
                "인계",
                "반환",
                "입양",
                "퇴원")) {
            return STATUS_COMPLETED;
        }

        if (containsAny(value,
                "상담",
                "예약",
                "진행")) {
            return STATUS_IN_COUNSELING;
        }

        return STATUS_OTHER;
    }

    private String normalizeSex(String value) {
        if (!hasText(value)) {
            return "정보없음";
        }

        return switch (value.trim().toUpperCase()) {
            case "F" -> "암컷";
            case "M" -> "수컷";
            case "Q" -> "미상";
            default -> value;
        };
    }

    private String normalizeNeutered(String value) {
        if (!hasText(value)) {
            return "미상";
        }

        return switch (value.trim().toUpperCase()) {
            case "Y" -> "예";
            case "N" -> "아니오";
            default -> "미상";
        };
    }

    private String extractLookupToken(String animalSlug) {
        if (!hasText(animalSlug)) {
            return "";
        }
        return animalSlug.trim();
    }

    private Long shelterIdFromCode(String shelterCode) {
        if (!hasText(shelterCode)) {
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private String normalizeMetroRegion(String value) {
        if (!hasText(value)) {
            return "";
        }

        return METRO_REGIONS.stream()
                .filter(value::startsWith)
                .findFirst()
                .orElse(value);
    }

    private String firstImage(Map<String, Object> source) {
        return imageUrls(source).stream().findFirst().orElse("");
    }

    private List<String> imageUrls(Map<String, Object> source) {
        Set<String> images = Arrays.asList("popfile1", "popfile2", "popfile3", "popfile4", "popfile5", "popfile", "filename")
                .stream()
                .map(key -> firstText(source, key))
                .filter(this::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return List.copyOf(images);
    }

    private LocalDate parseDate(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalDate parseBasicDate(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value, BASIC_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String urgencyBadge(Map<String, Object> item) {
        LocalDate today = LocalDate.now();
        LocalDate noticeEnd = parseBasicDate(firstText(item, "noticeEdt"));
        if (noticeEnd != null && !noticeEnd.isBefore(today) && !noticeEnd.isAfter(today.plusDays(7))) {
            return "마감임박";
        }

        LocalDate noticeStart = parseBasicDate(firstText(item, "noticeSdt"));
        if (noticeStart != null && !noticeStart.isAfter(today.minusDays(30))) {
            return "오래기다림";
        }

        return "";
    }

    private boolean containsAny(String value, String... candidates) {
        return Arrays.stream(candidates).anyMatch(value::contains);
    }
}
