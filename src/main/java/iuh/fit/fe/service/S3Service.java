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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
}
