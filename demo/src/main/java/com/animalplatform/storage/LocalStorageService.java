package com.animalplatform.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "spring.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path uploadRoot;
    private final String publicPathPrefix;
    private final long maxFileSizeBytes;

    @Autowired
    public LocalStorageService(
            @Value("${spring.storage.local.upload-root:uploads}") String uploadRoot,
            @Value("${spring.storage.local.public-path-prefix:/uploads}") String publicPathPrefix,
            @Value("${spring.storage.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.uploadRoot = Path.of(uploadRoot);
        this.publicPathPrefix = normalizePublicPrefix(publicPathPrefix);
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    LocalStorageService(String uploadRoot, String publicPathPrefix) {
        this(uploadRoot, publicPathPrefix, 5 * 1024 * 1024);
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String safeFolder = sanitizeFolder(folder);
            Path folderPath = uploadRoot.resolve(safeFolder).normalize();
            Files.createDirectories(folderPath);

            String filename = UUID.randomUUID() + UploadFileValidator.validateImage(file, maxFileSizeBytes);
            Path target = folderPath.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return publicPathPrefix + "/" + safeFolder + "/" + filename;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save uploaded file.", exception);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith(publicPathPrefix + "/")) {
            return;
        }

        try {
            String relativePath = fileUrl.substring((publicPathPrefix + "/").length());
            Path target = uploadRoot.resolve(relativePath).normalize();
            if (target.startsWith(uploadRoot.normalize())) {
                Files.deleteIfExists(target);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete uploaded file.", exception);
        }
    }

    private String sanitizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            return "files";
        }
        return folder.replace("\\", "/").replaceAll("[^a-zA-Z0-9/_-]", "").replaceAll("^/+", "");
    }

    private String normalizePublicPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "/uploads";
        }
        String normalized = prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

}
