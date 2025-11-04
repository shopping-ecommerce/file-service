package iuh.fit.fe.service;

import iuh.fit.fe.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class S3Service {
    final S3Client s3Client;
    final ImageValidationService imageValidationService;

    @Value("${aws.bucketName}")
    String bucketName;

    @Value("${aws.region}")
    String region;

    public List<String> uploadFile(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            log.warn("No files provided for upload");
            return List.of();
        }

        log.info("Starting upload process for {} file(s)", files.size());

        try {
            // Validate tất cả ảnh trước khi upload
            log.info("Step 1: Validating {} file(s) before upload", files.size());
            imageValidationService.validateImages(files);
            log.info("Step 2: All files passed validation, proceeding to upload");

            List<String> uploadedUrls = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                try {
                    log.info("Uploading file {}/{}: {}", i + 1, files.size(), file.getOriginalFilename());

                    String fileName = customizeFileName(file.getOriginalFilename());
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(fileName)
                                    .contentType(file.getContentType())
                                    .build(),
                            RequestBody.fromBytes(file.getBytes()));

                    String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                            bucketName, region, fileName);
                    uploadedUrls.add(fileUrl);
                    log.info("Successfully uploaded file {}/{}: {} -> {}",
                            i + 1, files.size(), file.getOriginalFilename(), fileName);

                } catch (IOException e) {
                    log.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                    throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
                }
            }

            log.info("Successfully uploaded {}/{} file(s)", uploadedUrls.size(), files.size());
            return uploadedUrls;

        } catch (AppException e) {
            log.error("Image validation failed: {}", e.getMessage());
            throw e; // Let the exception handler deal with it
        } catch (Exception e) {
            log.error("Failed to upload files to S3: {}", e.getMessage(), e);
            throw e;
        }
    }

    public byte[] downloadFile(String key) {
        ResponseBytes<GetObjectResponse> objectAsByte = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
        return objectAsByte.asByteArray();
    }

    public String customizeFileName(String originalFilename) {
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String baseName = originalFilename.substring(0, Math.min(originalFilename.lastIndexOf("."), 50));
        String uniqueSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return baseName + "_" + uniqueSuffix + fileExtension;
    }

    public List<String> deleteFiles(List<String> urlsOrKeys) {
        if (urlsOrKeys == null || urlsOrKeys.isEmpty()) {
            log.info("No URLs/keys provided for deletion.");
            return List.of();
        }
        log.info("Deleting {} item(s).", urlsOrKeys.size());

        List<String> keys = urlsOrKeys.stream().map(this::extractKey).toList();
        List<String> deletedAll = new ArrayList<>();

        // Chia lô 1000 đối tượng/lần theo giới hạn S3
        for (int i = 0; i < keys.size(); i += 1000) {
            List<String> chunk = keys.subList(i, Math.min(i + 1000, keys.size()));
            List<ObjectIdentifier> objects = chunk.stream()
                    .map(k -> ObjectIdentifier.builder().key(k).build())
                    .toList();

            DeleteObjectsResponse res = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objects).build())
                    .build());

            if (res.hasDeleted()) {
                deletedAll.addAll(res.deleted().stream().map(DeletedObject::key).toList());
            }
            if (res.hasErrors()) {
                res.errors().forEach(err ->
                        log.warn("Delete error - key: {}, code: {}, msg: {}", err.key(), err.code(), err.message()));
            }
        }
        log.info("Batch deleted {} object(s) from {}", deletedAll.size(), bucketName);
        return deletedAll;
    }

    // Nhận full URL (https://bucket.s3.region.amazonaws.com/a/b.png) hoặc key thuần (a/b.png) → trả key
    private String extractKey(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Key/URL must not be empty");
        String raw = input.trim();

        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            try {
                URI uri = URI.create(raw);
                String path = uri.getPath();               // "/a/b.png"
                raw = (path != null && path.startsWith("/")) ? path.substring(1) : path; // "a/b.png"
            } catch (Exception e) {
                // Nếu parse URL lỗi, fallback coi như key thuần
                log.warn("Failed to parse URL '{}', fallback to raw key", raw);
            }
        }
        // Decode URL-encoded (%2F, %20, ...)
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }
    public String uploadDoc(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn("No file provided for upload");
            return "";
        }

        // Validate file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF files are allowed.");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Invalid content type. Only application/pdf is allowed.");
        }

        log.info("Starting upload process for file: {}", originalFilename);

        try {
            // Generate a unique file name
            String fileName = customizeFileName(originalFilename);

            // Upload the file to S3
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            // Generate the file URL
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
            log.info("Successfully uploaded file: {} -> {}", originalFilename, fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload file {}: {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + originalFilename, e);
        }
    }
}