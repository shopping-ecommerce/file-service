package iuh.fit.fe.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Value("${aws.bucketName}")
    String bucketName;
    @Value("${aws.region}")
    String region;


    public List<String> uploadFile(List<MultipartFile> files) throws IOException {
        try {
            return files.stream().map(file -> {
                try {
                    String fileName = customizeFileName(file.getOriginalFilename());
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(fileName)
                                    .build(),
                            RequestBody.fromBytes(file.getBytes()));
                            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);

                } catch (IOException e) {
                    log.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage());
                    throw new RuntimeException(e);
                }
            }).toList();
        } catch (Exception e) {
            log.error("Failed to upload file {} to S3");
            throw e;
        }
    }

    public byte[] downloadFile(String key) {
        ResponseBytes<GetObjectResponse> objectAsByte = s3Client.getObjectAsBytes(GetObjectRequest.builder()
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
}
