package com.animalplatform.animal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.animalplatform.external.publicapi.PublicAnimalApiClient;
import com.animalplatform.statistics.domain.AnimalSnapshot;
import com.animalplatform.statistics.domain.SnapshotRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private PublicAnimalApiClient publicAnimalApiClient;

    @Test
    void findAnimalByDesertionNoReturnsFreshSnapshotWithoutCallingPublicApi() {
        AnimalSnapshot snapshot = snapshot("1001", LocalDateTime.now().minusHours(2));
        when(snapshotRepository.findByDesertionNo("1001")).thenReturn(Optional.of(snapshot));

        AnimalService animalService = new AnimalService(snapshotRepository, publicAnimalApiClient);

        Map<String, Object> result = animalService.findAnimalByDesertionNo("1001");

        assertThat(result)
                .containsEntry("desertionNo", "1001")
                .containsEntry("kindCd", "[개] 믹스견");
        verify(publicAnimalApiClient, never()).fetchByDesertionNo(any());
    }

    @Test
    void findAnimalByDesertionNoFallsBackToPublicApiWhenSnapshotMissing() {
        Map<String, Object> apiAnimal = Map.of(
                "desertionNo", "2002",
                "noticeNo", "NOTICE-2002",
                "kindCd", "[고양이] 코리안숏헤어",
                "processState", "보호중"
        );
        when(snapshotRepository.findByDesertionNo("2002")).thenReturn(Optional.empty());
        when(snapshotRepository.findByNoticeNo("2002")).thenReturn(Optional.empty());
        when(publicAnimalApiClient.fetchByDesertionNo("2002")).thenReturn(Optional.of(apiAnimal));
        when(snapshotRepository.findByDesertionNo("2002")).thenReturn(Optional.empty());

        AnimalService animalService = new AnimalService(snapshotRepository, publicAnimalApiClient);

        Map<String, Object> result = animalService.findAnimalByDesertionNo("2002");

        assertThat(result).containsEntry("desertionNo", "2002");
        verify(publicAnimalApiClient).fetchByDesertionNo("2002");
        verify(snapshotRepository).save(any(AnimalSnapshot.class));
    }

    @Test
    void findAnimalByDesertionNoThrows404WhenNotFoundAnywhere() {
        when(snapshotRepository.findByDesertionNo("missing")).thenReturn(Optional.empty());
        when(snapshotRepository.findByNoticeNo("missing")).thenReturn(Optional.empty());
        when(publicAnimalApiClient.fetchByDesertionNo("missing")).thenReturn(Optional.empty());

        AnimalService animalService = new AnimalService(snapshotRepository, publicAnimalApiClient);

        assertThatThrownBy(() -> animalService.findAnimalByDesertionNo("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode.value")
                .isEqualTo(404);
    }

    private AnimalSnapshot snapshot(String desertionNo, LocalDateTime collectedAt) {
        AnimalSnapshot snapshot = new AnimalSnapshot(desertionNo);
        snapshot.updateFrom(Map.of(
                "desertionNo", desertionNo,
                "noticeNo", "NOTICE-" + desertionNo,
                "kindCd", "[개] 믹스견",
                "processState", "보호중"
        ), collectedAt);
        return snapshot;
    }
}
