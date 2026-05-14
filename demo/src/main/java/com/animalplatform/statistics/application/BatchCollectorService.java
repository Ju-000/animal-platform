package com.animalplatform.statistics.application;

import com.animalplatform.admin.application.ApiHealthService;
import com.animalplatform.external.publicapi.PublicAnimalApiClient;
import com.animalplatform.external.publicapi.PublicAnimalApiClient.PublicApiPage;
import com.animalplatform.notification.application.NotificationService;
import com.animalplatform.statistics.domain.AnimalSnapshot;
import com.animalplatform.statistics.domain.AnimalStatusHistory;
import com.animalplatform.statistics.domain.AnimalStatusHistoryRepository;
import com.animalplatform.statistics.domain.BatchExecutionLog;
import com.animalplatform.statistics.domain.BatchExecutionLogRepository;
import com.animalplatform.statistics.domain.BatchExecutionStatus;
import com.animalplatform.statistics.domain.SnapshotRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class BatchCollectorService {

    private static final Logger log = LoggerFactory.getLogger(BatchCollectorService.class);
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_ATTEMPTS = 3;
    private static final int UNKNOWN_TOTAL_MAX_PAGE_PROBES = 5;

    private final PublicAnimalApiClient publicAnimalApiClient;
    private final SnapshotRepository snapshotRepository;
    private final AnimalStatusHistoryRepository animalStatusHistoryRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final ApiHealthService apiHealthService;
    private final BatchExecutionLogRepository batchExecutionLogRepository;

    public BatchCollectorService(
            PublicAnimalApiClient publicAnimalApiClient,
            SnapshotRepository snapshotRepository,
            AnimalStatusHistoryRepository animalStatusHistoryRepository,
            NotificationService notificationService,
            TransactionTemplate transactionTemplate,
            ApiHealthService apiHealthService,
            BatchExecutionLogRepository batchExecutionLogRepository
    ) {
        this.publicAnimalApiClient = publicAnimalApiClient;
        this.snapshotRepository = snapshotRepository;
        this.animalStatusHistoryRepository = animalStatusHistoryRepository;
        this.notificationService = notificationService;
        this.transactionTemplate = transactionTemplate;
        this.apiHealthService = apiHealthService;
        this.batchExecutionLogRepository = batchExecutionLogRepository;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runScheduledDailyCollection() {
        collectDailySnapshots();
    }

    public BatchRunResult collectDailySnapshots() {
        LocalDateTime collectedAt = LocalDateTime.now();
        Optional<BatchExecutionLog> runningBatch = batchExecutionLogRepository.findFirstByStatusAndStartedAtAfterOrderByStartedAtDesc(
                BatchExecutionStatus.RUNNING,
                collectedAt.minusHours(2)
        );
        if (runningBatch.isPresent()) {
            log.info("Batch already running, skipping. runningBatchId={}", runningBatch.get().getId());
            return new BatchRunResult(0, 0, 0, 0, collectedAt, collectedAt, "SKIPPED");
        }

        BatchExecutionLog executionLog = batchExecutionLogRepository.save(new BatchExecutionLog(collectedAt));
        int page = 1;
        int processedCount = 0;
        int insertedCount = 0;
        int updatedCount = 0;
        int totalCount = 0;
        int failedPageCount = 0;
        int attemptedPageCount = 0;
        Integer totalPages = null;
        List<String> errorSummaries = new ArrayList<>();
        List<StatusChangeEvent> statusChanges = new ArrayList<>();

        log.info("Starting animal snapshot collection. collectedAt={}", collectedAt);

        try {
            while (true) {
                attemptedPageCount++;
                PublicApiPage response = fetchPageWithRetry(page)
                        .orElse(null);
                if (response == null) {
                    failedPageCount++;
                    errorSummaries.add("page=" + page + " failed after " + MAX_ATTEMPTS + " attempts");
                    log.warn("Skipping failed animal snapshot page. page={}", page);
                    if (totalPages == null && page >= UNKNOWN_TOTAL_MAX_PAGE_PROBES) {
                        throw new IllegalStateException("Unable to fetch initial public API pages; total page count is unknown.");
                    }
                    if (totalPages != null && page >= totalPages) {
                        break;
                    }
                    page++;
                    continue;
                }
                List<Map<String, Object>> items = response.items();
                totalCount = Math.max(totalCount, response.totalCount());
                totalPages = Math.max(1, (int) Math.ceil((double) response.totalCount() / PAGE_SIZE));

                if (items.isEmpty()) {
                    break;
                }

                PageUpsertResult pageResult = upsertPage(items, collectedAt);
                insertedCount += pageResult.insertedCount();
                updatedCount += pageResult.updatedCount();
                statusChanges.addAll(pageResult.statusChanges());
                processedCount += items.size();

                if (processedCount % PAGE_SIZE == 0) {
                    log.info("Animal snapshot collection progress. processed={}, total={}", processedCount, totalCount);
                }

                if (page >= totalPages || processedCount >= response.totalCount()) {
                    break;
                }
                page++;
            }
        } catch (RuntimeException exception) {
            failedPageCount++;
            errorSummaries.add(exception.getMessage());
            completeExecutionLog(executionLog, BatchExecutionStatus.FAILED, processedCount, insertedCount + updatedCount, failedPageCount, errorSummaries);
            LocalDateTime failedAt = executionLog.getCompletedAt() == null ? LocalDateTime.now() : executionLog.getCompletedAt();
            apiHealthService.recordBatch(collectedAt, failedAt, processedCount, failedPageCount);
            alertOnConsecutiveFailures();
            throw exception;
        }

        BatchExecutionStatus status = resolveBatchStatus(failedPageCount, attemptedPageCount);
        completeExecutionLog(executionLog, status, processedCount, insertedCount + updatedCount, failedPageCount, errorSummaries);
        alertOnConsecutiveFailures();

        LocalDateTime completedAt = LocalDateTime.now();
        apiHealthService.recordBatch(collectedAt, completedAt, processedCount, failedPageCount);
        log.info(
                "Completed animal snapshot collection. processed={}, inserted={}, updated={}, failedPages={}, status={}, completedAt={}",
                processedCount,
                insertedCount,
                updatedCount,
                failedPageCount,
                status,
                completedAt
        );

        notificationService.notifyFavoriteStatusChanges(statusChanges);

        return new BatchRunResult(
                processedCount,
                insertedCount,
                updatedCount,
                statusChanges.size(),
                collectedAt,
                completedAt,
                status.name()
        );
    }

    private Optional<PublicApiPage> fetchPageWithRetry(int page) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return Optional.of(publicAnimalApiClient.fetchAbandonedAnimalPage(page, PAGE_SIZE));
            } catch (RuntimeException exception) {
                lastException = exception;
                log.warn(
                        "Failed to fetch animal snapshot page. page={}, attempt={}, maxAttempts={}",
                        page,
                        attempt,
                        MAX_ATTEMPTS,
                        exception
                );
                sleepBeforeRetry(attempt);
            }
        }
        log.error("Failed to fetch animal snapshot page after retries. page={}", page, lastException);
        return Optional.empty();
    }

    private BatchExecutionStatus resolveBatchStatus(int failedPageCount, int attemptedPageCount) {
        if (failedPageCount <= 0) {
            return BatchExecutionStatus.SUCCESS;
        }
        double failureRatio = attemptedPageCount == 0 ? 1.0 : failedPageCount / (double) attemptedPageCount;
        return failureRatio > 0.20 ? BatchExecutionStatus.PARTIAL : BatchExecutionStatus.SUCCESS;
    }

    private void completeExecutionLog(
            BatchExecutionLog executionLog,
            BatchExecutionStatus status,
            int totalFetched,
            int totalUpserted,
            int totalErrors,
            List<String> errorSummaries
    ) {
        String summary = String.join(System.lineSeparator(), errorSummaries);
        executionLog.complete(status, totalFetched, totalUpserted, totalErrors, summary);
        batchExecutionLogRepository.save(executionLog);
    }

    private void alertOnConsecutiveFailures() {
        List<BatchExecutionLog> recentRuns = batchExecutionLogRepository.findTop3ByOrderByStartedAtDesc();
        if (recentRuns.size() < 3) {
            return;
        }
        boolean allUnhealthy = recentRuns.stream()
                .allMatch(log -> log.getStatus() == BatchExecutionStatus.FAILED || log.getStatus() == BatchExecutionStatus.PARTIAL);
        if (allUnhealthy) {
            log.error("[포동포동] 배치 수집 연속 실패 감지");
        }
    }

    private PageUpsertResult upsertPage(List<Map<String, Object>> items, LocalDateTime collectedAt) {
        return transactionTemplate.execute(status -> {
            int insertedCount = 0;
            int updatedCount = 0;
            List<AnimalSnapshot> snapshots = new ArrayList<>();
            List<AnimalStatusHistory> histories = new ArrayList<>();
            List<StatusChangeEvent> statusChanges = new ArrayList<>();

            for (Map<String, Object> item : items) {
                String desertionNo = text(item, "desertionNo");
                if (!StringUtils.hasText(desertionNo)) {
                    continue;
                }

                AnimalSnapshot snapshot = snapshotRepository.findById(desertionNo)
                        .orElseGet(() -> new AnimalSnapshot(desertionNo));
                String previousStatus = snapshot.getProcessState();
                String newStatus = text(item, "processState");
                if (snapshot.getCollectedAt() == null) {
                    insertedCount++;
                } else {
                    updatedCount++;
                    if (hasStatusChanged(previousStatus, newStatus)) {
                        AnimalStatusHistory history = new AnimalStatusHistory(
                                desertionNo,
                                previousStatus,
                                newStatus,
                                collectedAt
                        );
                        histories.add(history);
                        statusChanges.add(new StatusChangeEvent(
                                desertionNo,
                                text(item, "noticeNo"),
                                text(item, "kindNm", "kindFullNm", "kindCd"),
                                firstImage(item),
                                previousStatus,
                                newStatus,
                                collectedAt
                        ));
                    }
                }
                snapshot.updateFrom(item, collectedAt);
                snapshots.add(snapshot);
            }

            snapshotRepository.saveAll(snapshots);
            animalStatusHistoryRepository.saveAll(histories);
            return new PageUpsertResult(insertedCount, updatedCount, statusChanges);
        });
    }

    private boolean hasStatusChanged(String previousStatus, String newStatus) {
        return StringUtils.hasText(previousStatus)
                && StringUtils.hasText(newStatus)
                && !previousStatus.trim().equals(newStatus.trim());
    }

    private void sleepBeforeRetry(int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            return;
        }
        try {
            Thread.sleep((long) Math.pow(2, attempt - 1) * 500L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying public API collection.", exception);
        }
    }

    private String text(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).trim();
        return "null".equalsIgnoreCase(text) ? "" : text;
    }

    private String text(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            String value = text(source, key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String firstImage(Map<String, Object> source) {
        return text(source, "popfile1", "popfile2", "popfile3", "popfile4", "popfile5", "popfile", "filename");
    }

    private record PageUpsertResult(int insertedCount, int updatedCount, List<StatusChangeEvent> statusChanges) {
    }

    public record BatchRunResult(
            int processedCount,
            int insertedCount,
            int updatedCount,
            int statusChangeCount,
            LocalDateTime collectedAt,
            LocalDateTime completedAt,
            String status
    ) {
    }

    public record StatusChangeEvent(
            String desertionNo,
            String noticeNo,
            String animalName,
            String thumbnailUrl,
            String previousStatus,
            String newStatus,
            LocalDateTime detectedAt
    ) {
    }
}
