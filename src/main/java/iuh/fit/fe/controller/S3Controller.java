package iuh.fit.fe.controller;

import iuh.fit.fe.dto.ApiResponse;
import iuh.fit.fe.service.S3Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
}
