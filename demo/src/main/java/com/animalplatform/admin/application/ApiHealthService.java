package com.animalplatform.admin.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ApiHealthService {

    private static final int MAX_CALLS = 100;
    private final Deque<ApiCallRecord> publicApiCalls = new ArrayDeque<>();
    private BatchRecord lastBatch;

    public synchronized void recordPublicApiCall(long responseMs, boolean success) {
        if (publicApiCalls.size() >= MAX_CALLS) {
            publicApiCalls.removeFirst();
        }
        publicApiCalls.addLast(new ApiCallRecord(LocalDateTime.now(), Math.max(0L, responseMs), success));
    }

    public synchronized void recordBatch(
            LocalDateTime startTime,
            LocalDateTime endTime,
            int recordsCollected,
            int errorCount
    ) {
        this.lastBatch = new BatchRecord(startTime, endTime, Math.max(0, recordsCollected), Math.max(0, errorCount));
    }

    public synchronized Map<String, Object> getPublicApiHealth() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<ApiCallRecord> recent = publicApiCalls.stream()
                .filter(record -> record.calledAt().isAfter(cutoff))
                .toList();

        long successCount = recent.stream().filter(ApiCallRecord::success).count();
        long failureCount = recent.size() - successCount;
        double successRate = recent.isEmpty() ? 100.0 : Math.round((successCount * 1000.0 / recent.size())) / 10.0;
        long avgResponseMs = recent.isEmpty()
                ? 0L
                : Math.round(recent.stream().mapToLong(ApiCallRecord::responseMs).average().orElse(0.0));
        ApiCallRecord last = publicApiCalls.peekLast();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("lastCalledAt", last == null ? "" : last.calledAt().toString());
        payload.put("avgResponseMs", avgResponseMs);
        payload.put("successRate", successRate);
        payload.put("successCount24h", successCount);
        payload.put("failureCount24h", failureCount);
        payload.put("status", resolveStatus(last, successRate, avgResponseMs, failureCount));
        payload.put("responseTimes", lastResponseTimes(20));
        return payload;
    }

    public synchronized Map<String, Object> getLastBatchHealth() {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (lastBatch == null) {
            payload.put("completedAt", "");
            payload.put("duration", "-");
            payload.put("recordsCollected", 0);
            payload.put("errorCount", 0);
            return payload;
        }

        payload.put("completedAt", lastBatch.endTime() == null ? "" : lastBatch.endTime().toString());
        payload.put("duration", formatDuration(lastBatch.startTime(), lastBatch.endTime()));
        payload.put("recordsCollected", lastBatch.recordsCollected());
        payload.put("errorCount", lastBatch.errorCount());
        return payload;
    }

    private List<Map<String, Object>> lastResponseTimes(int limit) {
        List<ApiCallRecord> records = new ArrayList<>(publicApiCalls);
        records.sort(Comparator.comparing(ApiCallRecord::calledAt));
        int fromIndex = Math.max(0, records.size() - limit);
        List<ApiCallRecord> tail = records.subList(fromIndex, records.size());
        List<Map<String, Object>> chart = new ArrayList<>();
        for (int index = 0; index < tail.size(); index++) {
            ApiCallRecord record = tail.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", String.valueOf(index + 1));
            item.put("calledAt", record.calledAt().toString());
            item.put("responseMs", record.responseMs());
            item.put("success", record.success());
            chart.add(item);
        }
        return chart;
    }

    private String resolveStatus(ApiCallRecord last, double successRate, long avgResponseMs, long failureCount) {
        if (last != null && !last.success()) {
            return "DOWN";
        }
        if (failureCount > 0 || successRate < 95.0 || avgResponseMs > 2_000L) {
            return "DEGRADED";
        }
        return "HEALTHY";
    }

    private String formatDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "-";
        }
        Duration duration = Duration.between(startTime, endTime);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).toSeconds();
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }

    private record ApiCallRecord(LocalDateTime calledAt, long responseMs, boolean success) {
    }

    private record BatchRecord(LocalDateTime startTime, LocalDateTime endTime, int recordsCollected, int errorCount) {
    }
}
