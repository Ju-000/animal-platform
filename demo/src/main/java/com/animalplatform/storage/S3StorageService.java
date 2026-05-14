package com.animalplatform.storage;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "spring.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;
    private final String publicBaseUrl;
    private final long maxFileSizeBytes;

    public S3StorageService(
            @Value("${spring.storage.s3.bucket-name:${S3_BUCKET_NAME:}}") String bucketName,
            @Value("${spring.storage.s3.region:${AWS_REGION:ap-northeast-2}}") String region,
            @Value("${spring.storage.s3.cloudfront-url:}") String cloudfrontUrl,
            @Value("${AWS_ACCESS_KEY_ID:}") String accessKeyId,
            @Value("${AWS_SECRET_ACCESS_KEY:}") String secretAccessKey,
            @Value("${spring.storage.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        // For production: set spring.storage.type=s3 and configure AWS credentials as environment variables.
        // Do NOT commit AWS keys to source control.
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(accessKeyId) || !StringUtils.hasText(secretAccessKey)) {
            throw new IllegalStateException("S3 storage requires S3_BUCKET_NAME, AWS_ACCESS_KEY_ID, and AWS_SECRET_ACCESS_KEY.");
        }
        this.bucketName = bucketName;
        this.region = region;
        this.publicBaseUrl = normalizeBaseUrl(StringUtils.hasText(cloudfrontUrl)
                ? cloudfrontUrl
                : "https://" + bucketName + ".s3." + region + ".amazonaws.com");
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String key = sanitizeFolder(folder) + "/" + UUID.randomUUID()
                + UploadFileValidator.validateImage(file, maxFileSizeBytes);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream")
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return publicBaseUrl + "/" + key;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded file.", exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to S3.", exception);
        }
    }

    @Override
    public void delete(String fileUrl) {
        String key = keyFromUrl(fileUrl);
        if (!StringUtils.hasText(key)) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file from S3.", exception);
        }
    }

    private String keyFromUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return "";
        }
        if (fileUrl.startsWith(publicBaseUrl + "/")) {
            return fileUrl.substring((publicBaseUrl + "/").length());
        }
        try {
            String path = URI.create(fileUrl).getPath();
            return path == null ? "" : path.replaceAll("^/+", "");
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String sanitizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            return "files";
        }
        return folder.replace("\\", "/").replaceAll("[^a-zA-Z0-9/_-]", "").replaceAll("^/+", "");
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

}
