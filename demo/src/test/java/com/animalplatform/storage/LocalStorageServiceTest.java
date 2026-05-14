package com.animalplatform.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeSavesFileAndReturnsPublicUrl() throws Exception {
        LocalStorageService storageService = new LocalStorageService(tempDir.toString(), "/uploads");
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "happy-dog.png",
                "image/png",
                "story-image".getBytes()
        );

        String url = storageService.store(file, "stories");

        assertThat(url).startsWith("/uploads/stories/");
        assertThat(url).endsWith(".png");

        String filename = url.substring("/uploads/stories/".length());
        Path savedFile = tempDir.resolve("stories").resolve(filename);
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(Files.readString(savedFile)).isEqualTo("story-image");
    }
}
