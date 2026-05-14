package com.animalplatform.storage;

import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

final class UploadFileValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private UploadFileValidator() {
    }

    static String validateImage(MultipartFile file, long maxBytes) {
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded image is too large.");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, WEBP, and GIF images are allowed.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, WEBP, and GIF images are allowed.");
        }
        return extension;
    }

    private static String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }
}
