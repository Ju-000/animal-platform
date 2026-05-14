package com.animalplatform.statistics.config;

import com.animalplatform.statistics.application.BatchCollectorService;
import com.animalplatform.statistics.domain.SnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(name = "app.batch.collect-on-startup", havingValue = "true")
public class LocalSnapshotStartupCollector implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSnapshotStartupCollector.class);

    private final SnapshotRepository snapshotRepository;
    private final BatchCollectorService batchCollectorService;

    public LocalSnapshotStartupCollector(
            SnapshotRepository snapshotRepository,
            BatchCollectorService batchCollectorService
    ) {
        this.snapshotRepository = snapshotRepository;
        this.batchCollectorService = batchCollectorService;
    }

    @Override
    public void run(ApplicationArguments args) {
        long snapshotCount = snapshotRepository.count();
        if (snapshotCount > 0) {
            log.info("AnimalSnapshot already populated. count={}", snapshotCount);
            return;
        }

        log.info("AnimalSnapshot empty on local startup. Running public API batch collection.");
        batchCollectorService.collectDailySnapshots();
    }
}
