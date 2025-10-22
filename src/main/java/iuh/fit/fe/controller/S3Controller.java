package iuh.fit.fe.controller;

import iuh.fit.fe.dto.ApiResponse;
import iuh.fit.fe.dto.DeleteRequest;
import iuh.fit.fe.dto.ImageValidationResult;
import iuh.fit.fe.service.ImageValidationService;
import iuh.fit.fe.service.S3Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RestController
@RequiredArgsConstructor
@Slf4j
public class S3Controller {
    S3Service s3Service;
    ImageValidationService imageValidationService;
    @PreAuthorize("hasAuthority('UPLOAD_FILE')")
    @PostMapping("/s3/upload")
    public ApiResponse<List<String>> uploadFile(@RequestParam("files") List<MultipartFile> files) throws IOException {
        log.error("Vào đi huhu");
        return ApiResponse.<List<String>>builder()
                .code(200)
                .message("File uploaded successfully")
                .result(s3Service.uploadFile(files))
                .build();
    }

    @GetMapping("/download/{fileName}")
    public ApiResponse<byte[]> downloadFile(@PathVariable String fileName) throws IOException {
        byte[] fileData = s3Service.downloadFile(fileName);
        return ApiResponse.<byte[]>builder()
                .code(200)
                .message("File downloaded successfully")
                .result(fileData)
                .build();
    }

    @PostMapping("/s3/delete")
    public ApiResponse<List<String>> deleteByUrl(@RequestBody DeleteRequest deleteRequest) {
        log.info("Deleting files with URLs: {}", deleteRequest.getUrls().toString());
        List<String> deletedKeys = s3Service.deleteFiles(deleteRequest.getUrls());
        return ApiResponse.<List<String>>builder()
                .code(200)
                .message("File(s) deleted successfully")
                .result(deletedKeys)
                .build();
    }

    @PostMapping(value = "/s3/validate-many", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<ImageValidationResult>> validateMany(@RequestPart("files") List<MultipartFile> files) throws IOException {
        log.info("[Moderation] validate-many: {} file(s)", files != null ? files.size() : 0);
        List<ImageValidationResult> results = imageValidationService.validateImagesDetailed(files);
        return ApiResponse.<List<ImageValidationResult>>builder()
                .code(200)
                .message("Validated images")
                .result(results)
                .build();
    }
}
