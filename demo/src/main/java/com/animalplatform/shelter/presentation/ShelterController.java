package com.animalplatform.shelter.presentation;



import com.animalplatform.common.api.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.animalplatform.common.api.ApiResponse;
import com.animalplatform.external.publicapi.PublicAnimalApiClient;
import com.animalplatform.shelter.application.ShelterDonationGoalService;
import com.animalplatform.shelter.domain.Shelter;
import com.animalplatform.shelter.domain.ShelterRepository;
import com.animalplatform.statistics.domain.AnimalSnapshot;
import com.animalplatform.statistics.domain.SnapshotRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
@Tag(name = "보호소", description = "해당 도메인 API")
@CommonApiResponses
@RestController
@RequestMapping("/api/shelters")
public class ShelterController {

    private static final Logger log = LoggerFactory.getLogger(ShelterController.class);
    private static final int SNAPSHOT_MAX_PAGES = 10;
    private static final int SNAPSHOT_PAGE_SIZE = 100;

    private final PublicAnimalApiClient publicAnimalApiClient;
    private final ShelterDonationGoalService shelterDonationGoalService;
    private final ShelterRepository shelterRepository;
    private final SnapshotRepository snapshotRepository;
    private final int defaultCapacity;

    public ShelterController(
            PublicAnimalApiClient publicAnimalApiClient,
            ShelterDonationGoalService shelterDonationGoalService,
            ShelterRepository shelterRepository,
            SnapshotRepository snapshotRepository,
            @Value("${app.shelter.default-capacity:30}") int defaultCapacity
    ) {
        this.publicAnimalApiClient = publicAnimalApiClient;
        this.shelterDonationGoalService = shelterDonationGoalService;
        this.shelterRepository = shelterRepository;
        this.snapshotRepository = snapshotRepository;
        this.defaultCapacity = defaultCapacity;
    }

    @GetMapping
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<List<Map<String, Object>>> getShelters(
            @RequestParam(required = false) CapacityStatus status
    ) {
        List<SnapshotRepository.ShelterSummary> shelterSummaries = snapshotRepository.findShelterSummaries();
        List<Map<String, Object>> shelters;

        if (shelterSummaries.isEmpty()) {
            log.warn("AnimalSnapshot shelter summary empty, falling back to public API");
            shelters = fetchShelterSummariesFromPublicApiFallback();
        } else {
            shelters = shelterSummaries.stream()
                    .map(this::toSnapshotShelterSummary)
                    .toList();
        }

        return ApiResponse.ok(filterAndSortShelters(shelters, status));
    }

    private List<Map<String, Object>> fetchShelterSummariesFromPublicApiFallback() {
        List<Map<String, Object>> animals = fetchAnimalSnapshotSafely();
        Map<String, Long> shelterAnimalCounts = animals.stream()
                .filter(this::isProtectedState)
                .collect(Collectors.groupingBy(item -> firstText(item, "careRegNo", "careNm"), Collectors.counting()));

        try {
            return publicAnimalApiClient.fetchShelters(1, 200).stream()
                    .map(item -> toShelterSummary(item, shelterAnimalCounts))
                    .toList();
        } catch (RestClientException exception) {
            log.warn("Failed to fetch shelters from public API. Falling back to local shelters.", exception);
            return shelterRepository.findAll().stream()
                    .map(this::toLocalShelterSummary)
                    .toList();
        }
    }

    private List<Map<String, Object>> fetchAnimalSnapshotSafely() {
        try {
            return publicAnimalApiClient.fetchAbandonedAnimalsSnapshot(
                    SNAPSHOT_MAX_PAGES,
                    SNAPSHOT_PAGE_SIZE
            );
        } catch (RestClientException exception) {
            log.warn("Failed to fetch animal snapshot for shelter capacity. Using zero counts.", exception);
            return List.of();
        }
    }

    private List<Map<String, Object>> filterAndSortShelters(List<Map<String, Object>> shelters, CapacityStatus status) {
        return shelters.stream()
                .filter(item -> status == null || status.name().equals(String.valueOf(item.get("capacityStatus"))))
                .sorted(Comparator
                        .comparing((Map<String, Object> item) -> String.valueOf(item.get("region")))
                        .thenComparing(item -> String.valueOf(item.get("name"))))
                .toList();
    }

    private Map<String, Object> toSnapshotShelterSummary(SnapshotRepository.ShelterSummary summary) {
        String careRegNo = firstNonBlank(summary.getCareRegNo(), summary.getCareNm(), summary.getCareAddr());
        long animalCount = summary.getAnimalCount() == null ? 0 : summary.getAnimalCount();
        long protectedCount = summary.getCriticalCount() == null ? 0 : summary.getCriticalCount();
        int estimatedCapacity = defaultCapacity;
        CapacityStatus capacityStatus = calculateCapacityStatus(animalCount, estimatedCapacity);

        Map<String, Object> shelter = new LinkedHashMap<>();
        shelter.put("id", shelterIdFromCode(careRegNo));
        shelter.put("careRegNo", careRegNo);
        shelter.put("name", firstNonBlank(summary.getCareNm(), "보호소"));
        shelter.put("region", normalizeRegion(firstNonBlank(summary.getOrgNm(), summary.getCareAddr())));
        shelter.put("address", firstNonBlank(summary.getCareAddr(), ""));
        shelter.put("phone", firstNonBlank(summary.getCareTel(), ""));
        shelter.put("donationAmount", animalCount * 18000);
        shelter.put("protectedAnimalCount", protectedCount);
        shelter.put("currentCount", animalCount);
        shelter.put("estimatedCapacity", estimatedCapacity);
        shelter.put("capacityStatus", capacityStatus.name());
        shelter.put("capacityStatusLabel", capacityStatus.label());
        shelter.put("donationGoal", shelterDonationGoalService.getGoalForSummary(careRegNo));
        return shelter;
    }

    private Map<String, Object> toLocalShelterSummary(Shelter shelter) {
        CapacityStatus capacityStatus = calculateCapacityStatus(0, defaultCapacity);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", shelter.getId());
        summary.put("careRegNo", shelter.getExternalShelterCode());
        summary.put("name", shelter.getName());
        summary.put("region", safeLocalText(shelter.getRegionCode(), "LOCAL"));
        summary.put("address", safeLocalText(shelter.getAddress(), ""));
        summary.put("phone", safeLocalText(shelter.getPhone(), ""));
        summary.put("donationAmount", 0);
        summary.put("protectedAnimalCount", 0);
        summary.put("currentCount", 0);
        summary.put("estimatedCapacity", defaultCapacity);
        summary.put("capacityStatus", capacityStatus.name());
        summary.put("capacityStatusLabel", capacityStatus.label());
        summary.put("donationGoal", shelterDonationGoalService.getGoalForSummary(shelter.getExternalShelterCode()));
        return summary;
    }

    private String safeLocalText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<AnimalSnapshot> findShelterSnapshots(String shelterIdentifier) {
        List<AnimalSnapshot> snapshots = snapshotRepository.findByCareRegNo(shelterIdentifier);
        if (!snapshots.isEmpty()) {
            return snapshots;
        }

        List<AnimalSnapshot> snapshotsByName = snapshotRepository.findByCareNm(shelterIdentifier);
        if (!snapshotsByName.isEmpty()) {
            return snapshotsByName;
        }

        return snapshotRepository.findShelterSummaries().stream()
                .filter(summary -> matchesShelterIdentifier(
                        firstNonBlank(summary.getCareRegNo(), summary.getCareNm(), summary.getCareAddr()),
                        shelterIdentifier
                ))
                .findFirst()
                .map(summary -> {
                    String careRegNo = firstNonBlank(summary.getCareRegNo(), summary.getCareNm(), summary.getCareAddr());
                    List<AnimalSnapshot> byCareRegNo = snapshotRepository.findByCareRegNo(careRegNo);
                    return byCareRegNo.isEmpty() ? snapshotRepository.findByCareNm(summary.getCareNm()) : byCareRegNo;
                })
                .orElse(List.of());
    }

    private AnimalSnapshot findShelterMetaSnapshot(String shelterIdentifier, List<AnimalSnapshot> snapshots) {
        return snapshotRepository.findFirstByCareRegNo(shelterIdentifier).orElse(snapshots.getFirst());
    }

    private boolean matchesShelterIdentifier(String careRegNo, String shelterIdentifier) {
        return careRegNo.equals(shelterIdentifier)
                || String.valueOf(shelterIdFromCode(careRegNo)).equals(shelterIdentifier);
    }

    private Map<String, Object> toSnapshotAnimalItem(AnimalSnapshot snapshot) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("desertionNo", firstNonBlank(snapshot.getDesertionNo(), ""));
        item.put("noticeNo", firstNonBlank(snapshot.getNoticeNo(), ""));
        item.put("kindCd", firstNonBlank(snapshot.getKindCd(), ""));
        item.put("kindNm", firstNonBlank(snapshot.getKindCd(), ""));
        item.put("kindFullNm", firstNonBlank(snapshot.getKindCd(), ""));
        item.put("colorCd", firstNonBlank(snapshot.getColorCd(), ""));
        item.put("age", firstNonBlank(snapshot.getAge(), ""));
        item.put("weight", firstNonBlank(snapshot.getWeight(), ""));
        item.put("sexCd", firstNonBlank(snapshot.getSexCd(), ""));
        item.put("neuterYn", firstNonBlank(snapshot.getNeuterYn(), ""));
        item.put("specialMark", firstNonBlank(snapshot.getSpecialMark(), ""));
        item.put("processState", firstNonBlank(snapshot.getProcessState(), ""));
        item.put("happenDt", firstNonBlank(snapshot.getHappenDt(), ""));
        item.put("happenPlace", firstNonBlank(snapshot.getHappenPlace(), ""));
        item.put("careRegNo", firstNonBlank(snapshot.getCareRegNo(), snapshot.getCareNm(), snapshot.getCareAddr()));
        item.put("careNm", firstNonBlank(snapshot.getCareNm(), ""));
        item.put("careAddr", firstNonBlank(snapshot.getCareAddr(), ""));
        item.put("careTel", firstNonBlank(snapshot.getCareTel(), ""));
        item.put("orgNm", firstNonBlank(snapshot.getOrgNm(), snapshot.getSido()));
        item.put("popfile", firstNonBlank(snapshot.getPopfile(), snapshot.getFilename()));
        item.put("filename", firstNonBlank(snapshot.getFilename(), ""));
        return item;
    }

    private boolean isExactState(AnimalSnapshot snapshot, String expectedState) {
        String processState = snapshot.getProcessState();
        return processState != null && processState.trim().equals(expectedState);
    }

    @GetMapping("/{shelterIdentifier}")
        @Operation(summary = "요청 처리", description = "해당 API 요청을 처리합니다.")
    public ApiResponse<Map<String, Object>> getShelterDetail(@PathVariable String shelterIdentifier) {
        List<AnimalSnapshot> snapshots = findShelterSnapshots(shelterIdentifier);
        if (snapshots.isEmpty()) {
            log.warn("No snapshot found for shelter {}, falling back to public API", shelterIdentifier);
            return ApiResponse.ok(getShelterDetailFromPublicApi(shelterIdentifier));
        }

        AnimalSnapshot shelterSnapshot = findShelterMetaSnapshot(shelterIdentifier, snapshots);
        List<Map<String, Object>> shelterAnimals = snapshots.stream()
                .map(this::toSnapshotAnimalItem)
                .toList();

        long totalCount = shelterAnimals.size();
        long protectedCount = snapshots.stream().filter(snapshot -> isExactState(snapshot, "보호중")).count();
        long adoptedCount = snapshots.stream().filter(snapshot -> isExactState(snapshot, "입양")).count();
        int estimatedCapacity = defaultCapacity;
        CapacityStatus capacityStatus = calculateCapacityStatus(totalCount, estimatedCapacity);
        long counselingCount = shelterAnimals.stream().filter(this::isCounselingState).count();
        long medicalNeedCount = shelterAnimals.stream().filter(this::hasMedicalNeed).count();
        long youngAnimalCount = shelterAnimals.stream().filter(this::isYoungAnimal).count();
        int donationAmount = calculateDonationAmount(totalCount, medicalNeedCount, youngAnimalCount);

        String careRegNo = firstNonBlank(shelterSnapshot.getCareRegNo(), shelterSnapshot.getCareNm(), shelterSnapshot.getCareAddr());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", shelterIdFromCode(careRegNo));
        detail.put("careRegNo", careRegNo);
        detail.put("name", firstNonBlank(shelterSnapshot.getCareNm(), "보호소"));
        detail.put("region", normalizeRegion(firstNonBlank(shelterSnapshot.getOrgNm(), shelterSnapshot.getCareAddr())));
        detail.put("address", firstNonBlank(shelterSnapshot.getCareAddr(), ""));
        detail.put("phone", firstNonBlank(shelterSnapshot.getCareTel(), ""));
        detail.put("manager", firstNonBlank(shelterSnapshot.getCareTel(), ""));
        detail.put("operatingHours", k("shelter.hours"));
        detail.put("noticeMessage", k("shelter.notice"));
        detail.put("totalCount", totalCount);
        detail.put("protectedAnimalCount", protectedCount);
        detail.put("adoptedCount", adoptedCount);
        detail.put("currentCount", totalCount);
        detail.put("estimatedCapacity", estimatedCapacity);
        detail.put("capacityStatus", capacityStatus.name());
        detail.put("capacityStatusLabel", capacityStatus.label());
        detail.put("counselingCount", counselingCount);
        detail.put("medicalNeedCount", medicalNeedCount);
        detail.put("youngAnimalCount", youngAnimalCount);
        detail.put("donationAmount", donationAmount);
        detail.put("donationGoal", shelterDonationGoalService.getGoalForSummary(careRegNo));
        detail.put("supportItems", buildSupportItems(totalCount, counselingCount, medicalNeedCount, youngAnimalCount));
        detail.put("usageItems", buildUsageItems(totalCount, medicalNeedCount, youngAnimalCount));
        detail.put("stories", buildStories(shelterAnimals));
        detail.put("animals", shelterAnimals.stream().limit(12).map(this::toShelterAnimal).toList());
        return ApiResponse.ok(detail);
    }

    private Map<String, Object> getShelterDetailFromPublicApi(String shelterIdentifier) {
        List<Map<String, Object>> shelterItems = publicAnimalApiClient.fetchShelters(1, 200);
        Map<String, Object> shelterItem = shelterItems.stream()
                .filter(item -> matchesShelterIdentifier(firstText(item, "careRegNo"), shelterIdentifier))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, k("shelter.notFound")));

        String careRegNo = firstText(shelterItem, "careRegNo");
        List<Map<String, Object>> shelterAnimals = publicAnimalApiClient.fetchAbandonedAnimalsSnapshot(
                        SNAPSHOT_MAX_PAGES,
                        SNAPSHOT_PAGE_SIZE
                ).stream()
                .filter(item -> careRegNo.equals(firstText(item, "careRegNo")))
                .toList();

        long protectedCount = shelterAnimals.stream().filter(this::isProtectedState).count();
        int estimatedCapacity = defaultCapacity;
        CapacityStatus capacityStatus = calculateCapacityStatus(protectedCount, estimatedCapacity);
        long counselingCount = shelterAnimals.stream().filter(this::isCounselingState).count();
        long medicalNeedCount = shelterAnimals.stream().filter(this::hasMedicalNeed).count();
        long youngAnimalCount = shelterAnimals.stream().filter(this::isYoungAnimal).count();
        int donationAmount = calculateDonationAmount(protectedCount, medicalNeedCount, youngAnimalCount);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", shelterIdFromCode(careRegNo));
        detail.put("careRegNo", careRegNo);
        detail.put("name", firstText(shelterItem, "careNm"));
        detail.put("region", normalizeRegion(firstText(shelterItem, "orgNm", "careAddr")));
        detail.put("address", firstText(shelterItem, "careAddr"));
        detail.put("phone", firstText(shelterItem, "careTel"));
        detail.put("manager", firstText(shelterItem, "careTel"));
        detail.put("operatingHours", k("shelter.hours"));
        detail.put("noticeMessage", k("shelter.notice"));
        detail.put("totalCount", shelterAnimals.size());
        detail.put("protectedAnimalCount", protectedCount);
        detail.put("adoptedCount", shelterAnimals.stream().filter(item -> "입양".equals(firstText(item, "processState"))).count());
        detail.put("currentCount", protectedCount);
        detail.put("estimatedCapacity", estimatedCapacity);
        detail.put("capacityStatus", capacityStatus.name());
        detail.put("capacityStatusLabel", capacityStatus.label());
        detail.put("counselingCount", counselingCount);
        detail.put("medicalNeedCount", medicalNeedCount);
        detail.put("youngAnimalCount", youngAnimalCount);
        detail.put("donationAmount", donationAmount);
        detail.put("donationGoal", shelterDonationGoalService.getGoalForSummary(careRegNo));
        detail.put("supportItems", buildSupportItems(protectedCount, counselingCount, medicalNeedCount, youngAnimalCount));
        detail.put("usageItems", buildUsageItems(protectedCount, medicalNeedCount, youngAnimalCount));
        detail.put("stories", buildStories(shelterAnimals));
        detail.put("animals", shelterAnimals.stream().limit(12).map(this::toShelterAnimal).toList());
        return detail;
    }

    private Map<String, Object> toShelterSummary(Map<String, Object> item, Map<String, Long> shelterAnimalCounts) {
        String careRegNo = firstText(item, "careRegNo");
        long protectedCount = shelterAnimalCounts.getOrDefault(careRegNo, 0L);
        int estimatedCapacity = defaultCapacity;
        CapacityStatus capacityStatus = calculateCapacityStatus(protectedCount, estimatedCapacity);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", shelterIdFromCode(careRegNo));
        summary.put("careRegNo", careRegNo);
        summary.put("name", firstText(item, "careNm"));
        summary.put("region", normalizeRegion(firstText(item, "orgNm", "careAddr")));
        summary.put("address", firstText(item, "careAddr"));
        summary.put("phone", firstText(item, "careTel"));
        summary.put("donationAmount", protectedCount * 18000);
        summary.put("protectedAnimalCount", protectedCount);
        summary.put("currentCount", protectedCount);
        summary.put("estimatedCapacity", estimatedCapacity);
        summary.put("capacityStatus", capacityStatus.name());
        summary.put("capacityStatusLabel", capacityStatus.label());
        summary.put("donationGoal", shelterDonationGoalService.getGoalForSummary(careRegNo));
        return summary;
    }

    private CapacityStatus calculateCapacityStatus(long currentCount, int capacity) {
        if (capacity <= 0) {
            if (currentCount > 30) {
                return CapacityStatus.CRITICAL;
            }
            if (currentCount > 20) {
                return CapacityStatus.WARNING;
            }
            return CapacityStatus.NORMAL;
        }

        double ratio = currentCount / (double) capacity;
        if (ratio > 0.8 || currentCount > 30) {
            return CapacityStatus.CRITICAL;
        }
        if (ratio > 0.6 || currentCount > 20) {
            return CapacityStatus.WARNING;
        }
        return CapacityStatus.NORMAL;
    }

    private Map<String, Object> toShelterAnimal(Map<String, Object> item) {
        Map<String, Object> animal = new LinkedHashMap<>();
        animal.put("id", parseLong(firstText(item, "desertionNo")));
        animal.put("name", displayAnimalName(item));
        animal.put("status", normalizeStatus(firstText(item, "processState")));
        animal.put("statusLabel", firstText(item, "processState"));
        animal.put("species", normalizeSpecies(firstText(item, "upKindCd", "upKindNm", "kindCd")));
        animal.put("kind", firstText(item, "kindCd"));
        animal.put("imageUrl", firstText(item, "popfile"));
        animal.put("noticeNumber", buildNoticeNumber(item));
        animal.put("region", normalizeRegion(firstText(item, "orgNm", "careAddr")));
        return animal;
    }

    private List<Map<String, Object>> buildSupportItems(
            long protectedCount,
            long counselingCount,
            long medicalNeedCount,
            long youngAnimalCount
    ) {
        List<Map<String, Object>> supportItems = new ArrayList<>();

        if (protectedCount > 0) {
            supportItems.add(Map.of(
                    "title", k("support.feed.title"),
                    "description", k("support.feed.description"),
                    "priority", protectedCount >= 10 ? "HIGH" : "MEDIUM"
            ));
        }
        if (youngAnimalCount > 0) {
            supportItems.add(Map.of(
                    "title", k("support.young.title"),
                    "description", k("support.young.description"),
                    "priority", "HIGH"
            ));
        }
        if (medicalNeedCount > 0) {
            supportItems.add(Map.of(
                    "title", k("support.medical.title"),
                    "description", k("support.medical.description"),
                    "priority", "HIGH"
            ));
        }
        if (counselingCount > 0) {
            supportItems.add(Map.of(
                    "title", k("support.counsel.title"),
                    "description", k("support.counsel.description"),
                    "priority", "MEDIUM"
            ));
        }
        if (supportItems.isEmpty()) {
            supportItems.add(Map.of(
                    "title", k("support.basic.title"),
                    "description", k("support.basic.description"),
                    "priority", "MEDIUM"
            ));
        }

        return supportItems;
    }

    private List<Map<String, Object>> buildUsageItems(long protectedCount, long medicalNeedCount, long youngAnimalCount) {
        long totalWeight = Math.max(1, protectedCount + medicalNeedCount + youngAnimalCount);
        int medicalShare = (int) Math.round((medicalNeedCount * 100.0) / totalWeight);
        int nutritionShare = (int) Math.round((youngAnimalCount * 100.0) / totalWeight);
        int careShare = Math.max(0, 100 - medicalShare - nutritionShare);

        return List.of(
                Map.of(
                        "title", k("usage.medical.title"),
                        "percent", medicalShare,
                        "description", k("usage.medical.description")
                ),
                Map.of(
                        "title", k("usage.young.title"),
                        "percent", nutritionShare,
                        "description", k("usage.young.description")
                ),
                Map.of(
                        "title", k("usage.care.title"),
                        "percent", careShare,
                        "description", k("usage.care.description")
                )
        );
    }

    private List<Map<String, Object>> buildStories(List<Map<String, Object>> shelterAnimals) {
        return shelterAnimals.stream()
                .filter(item -> !firstText(item, "specialMark").isBlank())
                .limit(4)
                .map(item -> {
                    Map<String, Object> story = new LinkedHashMap<>();
                    story.put("title", displayAnimalName(item) + " " + k("story.suffix"));
                    story.put("summary", trimSummary(firstText(item, "specialMark")));
                    story.put("noticeNumber", firstText(item, "noticeNo"));
                    story.put("animalSlug", createAnimalSlug(item));
                    story.put("date", firstText(item, "noticeSdt", "happenDt"));
                    return story;
                })
                .toList();
    }

    private int calculateDonationAmount(long protectedCount, long medicalNeedCount, long youngAnimalCount) {
        return (int) ((protectedCount * 18000) + (medicalNeedCount * 65000) + (youngAnimalCount * 22000));
    }

    private String createAnimalSlug(Map<String, Object> item) {
        return buildNoticeNumber(item);
    }

    private String buildNoticeNumber(Map<String, Object> item) {
        String notice = firstText(item, "noticeNo", "noticeNumber");
        if (!notice.isBlank()) {
            return notice.replace("[", "").replace("]", "").replace(' ', '-');
        }
        return String.valueOf(parseLong(firstText(item, "desertionNo")));
    }

    private String displayAnimalName(Map<String, Object> item) {
        String kind = firstText(item, "kindCd");
        int bracketIndex = kind.lastIndexOf("]");
        return bracketIndex >= 0 && bracketIndex + 1 < kind.length() ? kind.substring(bracketIndex + 1).trim() : kind;
    }

    private String trimSummary(String value) {
        return value.length() > 90 ? value.substring(0, 90) + "..." : value;
    }

    private boolean isProtectedState(Map<String, Object> item) {
        String state = firstText(item, "processState");
        return containsAny(state, k("word.protected"), k("word.treatment"), k("word.notice"));
    }

    private boolean isCounselingState(Map<String, Object> item) {
        String state = firstText(item, "processState");
        return containsAny(state, k("word.adoption"), k("word.transfer"), k("word.closed"), k("word.counseling"));
    }

    private boolean hasMedicalNeed(Map<String, Object> item) {
        String specialMark = firstText(item, "specialMark");
        return containsAny(
                specialMark,
                k("word.injury"),
                k("word.treatment"),
                k("word.surgery"),
                k("word.fracture"),
                k("word.inflammation")
        );
    }

    private boolean isYoungAnimal(Map<String, Object> item) {
        String age = firstText(item, "age");
        return containsAny(age, k("word.month"), "60" + k("word.day"), k("word.under"));
    }

    private boolean containsAny(String source, String... keywords) {
        if (source == null || source.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSpecies(String value) {
        if (containsAny(value, k("word.dog"), k("word.puppy"))) {
            return "DOG";
        }
        if (containsAny(value, k("word.cat"))) {
            return "CAT";
        }
        return value == null ? "" : value.toUpperCase();
    }

    private String normalizeStatus(String value) {
        if (containsAny(value, k("word.adoption"), k("word.transfer"), k("word.closed"), k("word.counseling"))) {
            return "IN_COUNSELING";
        }
        if (containsAny(value, k("word.protected"), k("word.treatment"), k("word.notice"))) {
            return "ADOPTABLE";
        }
        return "SUPPORT_NEEDED";
    }

    private String normalizeRegion(String value) {
        if (containsAny(value, k("region.seoul.short"), k("region.seoul"))) return k("region.seoul");
        if (containsAny(value, k("region.busan.short"), k("region.busan"))) return k("region.busan");
        if (containsAny(value, k("region.daegu.short"), k("region.daegu"))) return k("region.daegu");
        if (containsAny(value, k("region.incheon.short"), k("region.incheon"))) return k("region.incheon");
        if (containsAny(value, k("region.gwangju.short"), k("region.gwangju"))) return k("region.gwangju");
        if (containsAny(value, k("region.daejeon.short"), k("region.daejeon"))) return k("region.daejeon");
        if (containsAny(value, k("region.ulsan.short"), k("region.ulsan"))) return k("region.ulsan");
        if (containsAny(value, k("region.sejong.short"), k("region.sejong"))) return k("region.sejong");
        if (containsAny(value, k("region.gyeonggi.short"), k("region.gyeonggi"))) return k("region.gyeonggi");
        if (containsAny(value, k("region.gangwon.short"), k("region.gangwon"))) return k("region.gangwon");
        if (containsAny(value, k("region.chungbuk.short"), k("region.chungbuk"))) return k("region.chungbuk");
        if (containsAny(value, k("region.chungnam.short"), k("region.chungnam"))) return k("region.chungnam");
        if (containsAny(value, k("region.jeonbuk.short"), k("region.jeonbuk"))) return k("region.jeonbuk");
        if (containsAny(value, k("region.jeonnam.short"), k("region.jeonnam"))) return k("region.jeonnam");
        if (containsAny(value, k("region.gyeongbuk.short"), k("region.gyeongbuk"))) return k("region.gyeongbuk");
        if (containsAny(value, k("region.gyeongnam.short"), k("region.gyeongnam"))) return k("region.gyeongnam");
        if (containsAny(value, k("region.jeju.short"), k("region.jeju"))) return k("region.jeju");
        return value;
    }

    private long shelterIdFromCode(String shelterCode) {
        return Math.abs((long) shelterCode.hashCode());
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception exception) {
            return Math.abs((long) value.hashCode());
        }
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String k(String key) {
        return switch (key) {
            case "shelter.notFound" -> "보호소 정보를 찾을 수 없습니다.";
            case "shelter.hours" -> "전화 문의 후 방문 권장";
            case "shelter.notice" -> "전화 문의는 보호소 운영시간 확인 후 이용 바랍니다.";
            case "story.suffix" -> "보호 이야기";
            case "support.feed.title" -> "사료와 간식";
            case "support.feed.description" -> "현재 보호 중인 동물이 있어 기본 생활 지원이 필요합니다.";
            case "support.young.title" -> "분유와 영양식";
            case "support.young.description" -> "어린 개체가 있어 분유, 영양식, 보온용품 지원이 필요합니다.";
            case "support.medical.title" -> "치료비 지원";
            case "support.medical.description" -> "특이사항에 치료 관련 내용이 보여 의료비 지원이 우선입니다.";
            case "support.counsel.title" -> "입양 상담 운영비";
            case "support.counsel.description" -> "상담 진행 중인 동물이 있어 상담과 홍보 운영 지원이 필요합니다.";
            case "support.basic.title" -> "기본 운영 후원";
            case "support.basic.description" -> "정기적인 사료, 소모품, 위생용품 지원이 필요합니다.";
            case "usage.medical.title" -> "치료와 검진";
            case "usage.medical.description" -> "치료 관련 특이사항이 있는 동물 비중을 바탕으로 계산합니다.";
            case "usage.young.title" -> "분유와 영양식";
            case "usage.young.description" -> "어린 개체 수를 기준으로 영양 지원 비중을 계산합니다.";
            case "usage.care.title" -> "사료와 위생용품";
            case "usage.care.description" -> "현재 보호 중인 전체 동물 수를 기준으로 기본 운영 비중을 계산합니다.";
            case "word.protected" -> "보호";
            case "word.treatment" -> "치료";
            case "word.notice" -> "공고";
            case "word.adoption" -> "입양";
            case "word.transfer" -> "분양";
            case "word.closed" -> "종료";
            case "word.counseling" -> "상담";
            case "word.injury" -> "부상";
            case "word.surgery" -> "수술";
            case "word.fracture" -> "골절";
            case "word.inflammation" -> "염증";
            case "word.month" -> "개월";
            case "word.day" -> "일";
            case "word.under" -> "미만";
            case "word.dog" -> "개";
            case "word.puppy" -> "강아지";
            case "word.cat" -> "고양이";
            case "region.seoul.short" -> "서울";
            case "region.seoul" -> "서울특별시";
            case "region.busan.short" -> "부산";
            case "region.busan" -> "부산광역시";
            case "region.daegu.short" -> "대구";
            case "region.daegu" -> "대구광역시";
            case "region.incheon.short" -> "인천";
            case "region.incheon" -> "인천광역시";
            case "region.gwangju.short" -> "광주";
            case "region.gwangju" -> "광주광역시";
            case "region.daejeon.short" -> "대전";
            case "region.daejeon" -> "대전광역시";
            case "region.ulsan.short" -> "울산";
            case "region.ulsan" -> "울산광역시";
            case "region.sejong.short" -> "세종";
            case "region.sejong" -> "세종특별자치시";
            case "region.gyeonggi.short" -> "경기";
            case "region.gyeonggi" -> "경기도";
            case "region.gangwon.short" -> "강원";
            case "region.gangwon" -> "강원특별자치도";
            case "region.chungbuk.short" -> "충북";
            case "region.chungbuk" -> "충청북도";
            case "region.chungnam.short" -> "충남";
            case "region.chungnam" -> "충청남도";
            case "region.jeonbuk.short" -> "전북";
            case "region.jeonbuk" -> "전북특별자치도";
            case "region.jeonnam.short" -> "전남";
            case "region.jeonnam" -> "전라남도";
            case "region.gyeongbuk.short" -> "경북";
            case "region.gyeongbuk" -> "경상북도";
            case "region.gyeongnam.short" -> "경남";
            case "region.gyeongnam" -> "경상남도";
            case "region.jeju.short" -> "제주";
            case "region.jeju" -> "제주특별자치도";
            default -> key;
        };
    }

    private enum CapacityStatus {
        CRITICAL("위험"),
        WARNING("주의"),
        NORMAL("여유");

        private final String label;

        CapacityStatus(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }
}
