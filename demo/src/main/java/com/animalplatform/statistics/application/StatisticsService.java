package com.animalplatform.statistics.application;

import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.statistics.domain.AnimalSnapshot;
import com.animalplatform.statistics.domain.SnapshotRepository;
import com.animalplatform.statistics.domain.SnapshotRepository.CountByValue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    private final SnapshotRepository snapshotRepository;
    private final DonationRepository donationRepository;

    public StatisticsService(SnapshotRepository snapshotRepository, DonationRepository donationRepository) {
        this.snapshotRepository = snapshotRepository;
        this.donationRepository = donationRepository;
    }

    public Map<String, Object> getSummary(String startDate, String endDate) {
        DateRange dateRange = parseDateRange(startDate, endDate);
        List<StatusCount> statusCounts = loadStatusCounts(dateRange);
        List<StatusCount> sexCounts = loadSexCounts(dateRange);

        long totalCount = statusCounts.stream().mapToLong(StatusCount::count).sum();
        long protectedCount = sumMatching(statusCounts, "보호", "공고");
        long supportNeededCount = sumMatching(statusCounts, "치료", "후원");
        long counselingCount = sumMatching(statusCounts, "상담");
        long adoptedCount = sumMatching(statusCounts, "입양", "분양", "반환", "인도");
        long euthanasiaCount = sumMatching(statusCounts, "안락사", "자연사", "폐사");
        long knownStatusCount = protectedCount + supportNeededCount + counselingCount + adoptedCount + euthanasiaCount;
        long otherCount = Math.max(totalCount - knownStatusCount, 0);

        BigDecimal donationTotal = donationRepository.findAll().stream()
                .map(donation -> donation.getAmount() == null ? BigDecimal.ZERO : donation.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("보호중", protectedCount);
        statusDistribution.put("후원필요", supportNeededCount);
        statusDistribution.put("상담중", counselingCount);
        statusDistribution.put("입양완료", adoptedCount);
        statusDistribution.put("안락사", euthanasiaCount);
        statusDistribution.put("기타", otherCount);

        Map<String, Long> sexDistribution = new LinkedHashMap<>();
        sexDistribution.put("수컷", sumExact(sexCounts, "M"));
        sexDistribution.put("암컷", sumExact(sexCounts, "F"));
        sexDistribution.put("미상", totalCount - sexDistribution.get("수컷") - sexDistribution.get("암컷"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalAnimalsCount", totalCount);
        payload.put("protectedAnimalsCount", protectedCount);
        payload.put("supportNeededAnimalsCount", supportNeededCount);
        payload.put("counselingAnimalsCount", counselingCount);
        payload.put("adoptableAnimalsCount", protectedCount + supportNeededCount + counselingCount);
        payload.put("adoptedAnimalsCount", adoptedCount);
        payload.put("euthanasiaAnimalsCount", euthanasiaCount);
        payload.put("adoptionRate", percentage(adoptedCount, totalCount));
        payload.put("euthanasiaRate", percentage(euthanasiaCount, totalCount));
        payload.put("statusDistribution", statusDistribution);
        payload.put("sexDistribution", sexDistribution);
        payload.put("adoptionApplicationCount", counselingCount);
        payload.put("donationAmountTotal", donationTotal);
        payload.put("snapshotSize", totalCount);
        payload.put("snapshotSource", "LOCAL_DB");
        payload.put("lastCollectedAt", findLastCollectedAt());
        return payload;
    }

    public List<Map<String, Object>> getRegions(String startDate, String endDate) {
        DateRange dateRange = parseDateRange(startDate, endDate);
        return loadRegionCounts(dateRange).stream()
                .filter(count -> !count.label().isBlank())
                .map(count -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("label", count.label());
                    item.put("value", count.count());
                    return item;
                })
                .sorted(Comparator.comparingLong(item -> -Long.parseLong(String.valueOf(item.get("value")))))
                .toList();
    }

    private List<StatusCount> loadStatusCounts(DateRange dateRange) {
        if (dateRange.hasFilter()) {
            return loadSnapshots(dateRange).stream()
                    .collect(
                            LinkedHashMap<String, Long>::new,
                            (map, snapshot) -> map.merge(nullToBlank(snapshot.getProcessState()), 1L, Long::sum),
                            Map::putAll
                    )
                    .entrySet()
                    .stream()
                    .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
                    .toList();
        }
        return snapshotRepository.countByProcessState().stream()
                .map(this::toStatusCount)
                .toList();
    }

    private List<StatusCount> loadSexCounts(DateRange dateRange) {
        if (dateRange.hasFilter()) {
            return loadSnapshots(dateRange).stream()
                    .collect(
                            LinkedHashMap<String, Long>::new,
                            (map, snapshot) -> map.merge(nullToBlank(snapshot.getSexCd()), 1L, Long::sum),
                            Map::putAll
                    )
                    .entrySet()
                    .stream()
                    .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
                    .toList();
        }
        return snapshotRepository.countBySexCd().stream()
                .map(this::toStatusCount)
                .toList();
    }

    private List<StatusCount> loadRegionCounts(DateRange dateRange) {
        if (dateRange.hasFilter()) {
            return loadSnapshots(dateRange).stream()
                    .collect(
                            LinkedHashMap<String, Long>::new,
                            (map, snapshot) -> map.merge(nullToBlank(snapshot.getSido()), 1L, Long::sum),
                            Map::putAll
                    )
                    .entrySet()
                    .stream()
                    .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
                    .toList();
        }
        LocalDateTime collectedAfter = snapshotRepository.findTopByOrderByCollectedAtDesc()
                .map(AnimalSnapshot::getCollectedAt)
                .map(time -> time.minusSeconds(1))
                .orElse(LocalDateTime.MIN);
        return snapshotRepository.countBySidoAndCollectedAtAfter(collectedAfter).stream()
                .map(this::toStatusCount)
                .toList();
    }

    private List<AnimalSnapshot> loadSnapshots(DateRange dateRange) {
        String start = dateRange.start() == null ? "00000000" : dateRange.start().format(DateTimeFormatter.BASIC_ISO_DATE);
        String end = dateRange.end() == null ? "99999999" : dateRange.end().format(DateTimeFormatter.BASIC_ISO_DATE);
        return snapshotRepository.findAllByHappenDtBetween(start, end);
    }

    private StatusCount toStatusCount(CountByValue value) {
        return new StatusCount(nullToBlank(value.getLabel()), value.getCount() == null ? 0L : value.getCount());
    }

    private long sumMatching(List<StatusCount> counts, String... keywords) {
        return counts.stream()
                .filter(count -> containsAny(count.label(), keywords))
                .mapToLong(StatusCount::count)
                .sum();
    }

    private long sumExact(List<StatusCount> counts, String expected) {
        return counts.stream()
                .filter(count -> expected.equalsIgnoreCase(count.label()))
                .mapToLong(StatusCount::count)
                .sum();
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((double) numerator * 100 / denominator)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String findLastCollectedAt() {
        return snapshotRepository.findTopByOrderByCollectedAtDesc()
                .map(AnimalSnapshot::getCollectedAt)
                .map(LocalDateTime::toString)
                .orElse(null);
    }

    private DateRange parseDateRange(String startDate, String endDate) {
        return new DateRange(parseDate(startDate), parseDate(endDate));
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replaceAll("[^0-9-]", "");
        try {
            if (normalized.matches("\\d{8}")) {
                return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE);
            }
            if (normalized.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(normalized);
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private record StatusCount(String label, long count) {
    }

    private record DateRange(LocalDate start, LocalDate end) {
        private boolean hasFilter() {
            return start != null || end != null;
        }
    }
}
