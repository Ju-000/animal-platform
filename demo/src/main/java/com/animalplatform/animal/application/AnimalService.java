package com.animalplatform.animal.application;

import com.animalplatform.external.publicapi.PublicAnimalApiClient;
import com.animalplatform.statistics.domain.AnimalSnapshot;
import com.animalplatform.statistics.domain.SnapshotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnimalService {

    private static final Logger log = LoggerFactory.getLogger(AnimalService.class);
    private static final long SNAPSHOT_TTL_HOURS = 24L;

    private final SnapshotRepository snapshotRepository;
    private final PublicAnimalApiClient publicAnimalApiClient;

    public AnimalService(SnapshotRepository snapshotRepository, PublicAnimalApiClient publicAnimalApiClient) {
        this.snapshotRepository = snapshotRepository;
        this.publicAnimalApiClient = publicAnimalApiClient;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findAnimalByDesertionNo(String desertionNo) {
        return snapshotRepository.findByDesertionNo(desertionNo)
                .or(() -> snapshotRepository.findByNoticeNo(desertionNo))
                .map(snapshot -> {
                    if (isStale(snapshot)) {
                        refreshSnapshotAsync(desertionNo);
                    }
                    return toPublicApiShape(snapshot);
                })
                .orElseGet(() -> fetchFromPublicApiOrThrow(desertionNo));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecommendedAnimals(String orgNm, int size) {
        LocalDate today = LocalDate.now();
        String todayText = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        String urgentDate = today.plusDays(7).format(DateTimeFormatter.BASIC_ISO_DATE);
        String longWaitDate = today.minusDays(30).format(DateTimeFormatter.BASIC_ISO_DATE);
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(size, 50)));

        List<AnimalSnapshot> snapshots;
        if (orgNm != null && !orgNm.isBlank()) {
            snapshots = snapshotRepository.findRecommendedByRegion(orgNm.trim(), todayText, urgentDate, longWaitDate, page);
        } else {
            snapshots = snapshotRepository.findRecommended(todayText, urgentDate, longWaitDate, page);
        }

        return snapshots.stream()
                .map(this::toPublicApiShape)
                .toList();
    }

    private Map<String, Object> fetchFromPublicApiOrThrow(String desertionNo) {
        return publicAnimalApiClient.fetchByDesertionNo(desertionNo)
                .map(item -> {
                    upsertSnapshot(item);
                    return item;
                })
                .orElseThrow(() -> new AnimalNotFoundException(desertionNo));
    }

    private void refreshSnapshotAsync(String desertionNo) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                publicAnimalApiClient.fetchByDesertionNo(desertionNo).ifPresent(this::upsertSnapshot);
            } catch (RuntimeException exception) {
                log.warn("Failed to refresh stale animal snapshot. desertionNo={}", desertionNo, exception);
            }
        });
    }

    @Transactional
    public void upsertSnapshot(Map<String, Object> item) {
        String desertionNo = text(item, "desertionNo");
        if (desertionNo.isBlank()) {
            return;
        }
        AnimalSnapshot snapshot = snapshotRepository.findByDesertionNo(desertionNo)
                .orElseGet(() -> new AnimalSnapshot(desertionNo));
        snapshot.updateFrom(item, LocalDateTime.now());
        snapshotRepository.save(snapshot);
    }

    private boolean isStale(AnimalSnapshot snapshot) {
        return snapshot.getCollectedAt() == null
                || snapshot.getCollectedAt().isBefore(LocalDateTime.now().minusHours(SNAPSHOT_TTL_HOURS));
    }

    public Map<String, Object> toPublicApiShape(AnimalSnapshot snapshot) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("desertionNo", value(snapshot.getDesertionNo()));
        item.put("noticeNo", value(snapshot.getNoticeNo()));
        item.put("kindCd", value(snapshot.getKindCd()));
        item.put("kindNm", value(snapshot.getKindCd()));
        item.put("kindFullNm", value(snapshot.getKindCd()));
        item.put("colorCd", value(snapshot.getColorCd()));
        item.put("age", value(snapshot.getAge()));
        item.put("weight", value(snapshot.getWeight()));
        item.put("sexCd", value(snapshot.getSexCd()));
        item.put("neuterYn", value(snapshot.getNeuterYn()));
        item.put("specialMark", value(snapshot.getSpecialMark()));
        item.put("processState", value(snapshot.getProcessState()));
        item.put("happenDt", value(snapshot.getHappenDt()));
        item.put("happenPlace", value(snapshot.getHappenPlace()));
        item.put("noticeSdt", value(snapshot.getNoticeSdt()));
        item.put("noticeEdt", value(snapshot.getNoticeEdt()));
        item.put("careNm", value(snapshot.getCareNm()));
        item.put("careAddr", value(snapshot.getCareAddr()));
        item.put("careTel", value(snapshot.getCareTel()));
        item.put("careRegNo", value(snapshot.getCareRegNo()));
        item.put("orgNm", value(firstNonBlank(snapshot.getOrgNm(), snapshot.getSido())));
        item.put("popfile", value(snapshot.getPopfile()));
        item.put("popfile1", value(firstNonBlank(snapshot.getPopfile(), snapshot.getFilename())));
        item.put("filename", value(snapshot.getFilename()));
        return item;
    }

    private String text(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String... values) {
        for (String candidate : values) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }
}
