package iuh.fit.fe.service;

import iuh.fit.fe.exception.AppException;
import iuh.fit.fe.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ImageValidationService {

    final RekognitionClient rekognitionClient;

    @Value("${aws.rekognition.confidence-threshold:80.0}")
    Float confidenceThreshold;

    // Các nhãn bị cấm
    private static final Set<String> BLOCKED_LABELS = new HashSet<>(Arrays.asList(
            "Explicit Nudity", "Nudity", "Graphic Male Nudity", "Graphic Female Nudity",
            "Sexual Activity", "Illustrated Explicit Nudity", "Adult Toys",
            "Violence", "Graphic Violence", "Physical Violence", "Weapon Violence",
            "Weapons", "Self Injury", "Emaciated Bodies", "Corpses", "Hanging",
            "Visually Disturbing", "Explosions And Blasts"
    ));

    /**
     * Kiểm tra tính hợp lệ của ảnh dựa trên nội dung
     * @param file File ảnh cần kiểm tra
     * @throws AppException nếu ảnh chứa nội dung không phù hợp
     * @throws IOException nếu có lỗi đọc file
     */
    public void validateImage(MultipartFile file) throws IOException {
        log.info("Validating image: {}", file.getOriginalFilename());

        // Kiểm tra xem có phải file ảnh không
        if (!isImageFile(file)) {
            throw new AppException(ErrorCode.FILE_NOT_VALID);
        }

        try {
            // Detect moderation labels
            DetectModerationLabelsResponse moderationResponse = detectModerationLabels(file);

            // Log response để debug
            log.debug("Rekognition response for {}: {} labels found",
                    file.getOriginalFilename(),
                    moderationResponse.moderationLabels() != null ? moderationResponse.moderationLabels().size() : 0);

            // Kiểm tra nếu không có labels
            if (moderationResponse.moderationLabels() == null || moderationResponse.moderationLabels().isEmpty()) {
                log.info("Image validation passed (no moderation labels): {}", file.getOriginalFilename());
                return;
            }

            // Kiểm tra các nhãn nguy hiểm
            List<ModerationLabel> unsafeLabels = moderationResponse.moderationLabels().stream()
                    .filter(label -> label.confidence() >= confidenceThreshold)
                    .filter(label -> {
                        String labelName = label.name();
                        String parentName = label.parentName();
                        return BLOCKED_LABELS.contains(labelName) ||
                                (parentName != null && BLOCKED_LABELS.contains(parentName));
                    })
                    .collect(Collectors.toList());

            if (!unsafeLabels.isEmpty()) {
                String reasons = unsafeLabels.stream()
                        .map(label -> String.format("%s (%.2f%%)", label.name(), label.confidence()))
                        .collect(Collectors.joining(", "));
                log.warn("Image rejected - File: {}, Reasons: {}", file.getOriginalFilename(), reasons);
                throw new AppException(ErrorCode.IMAGE_CONTENT_NOT_ALLOWED);
            }

//            log.info("Image validation passed: {}", file.getOriginalFilename());

        } catch (AppException e) {
            throw e; // Re-throw validation errors
        } catch (RekognitionException e) {
            log.error("AWS Rekognition error for file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new AppException(ErrorCode.FILE_NOT_VALID);
        } catch (Exception e) {
            log.error("Unexpected error validating image {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new AppException(ErrorCode.FILE_NOT_VALID);
        }
    }

    /**
     * Gọi AWS Rekognition để phát hiện nội dung không phù hợp
     */
    private DetectModerationLabelsResponse detectModerationLabels(MultipartFile file) throws IOException {
        DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
                .image(Image.builder()
                        .bytes(SdkBytes.fromByteArray(file.getBytes()))
                        .build())
                .minConfidence(confidenceThreshold)
                .build();

        return rekognitionClient.detectModerationLabels(request);
    }

    /**
     * Kiểm tra xem file có phải là ảnh không
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/");
    }

    /**
     * Validate nhiều ảnh cùng lúc
     */
    public void validateImages(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            log.warn("No files to validate");
            return;
        }

        log.info("Starting validation for {} file(s)", files.size());
        int successCount = 0;

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            log.info("Validating file {}/{}: {}", i + 1, files.size(), file.getOriginalFilename());
            try {
                validateImage(file);
                successCount++;
            } catch (AppException e) {
                log.error("Validation failed for file {}: {}", file.getOriginalFilename(), e.getMessage());
                throw e; // Stop at first failure
            }
        }

        log.info("Successfully validated {}/{} file(s)", successCount, files.size());
    }
}