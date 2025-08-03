package iuh.fit.fe.controller;

import iuh.fit.fe.dto.ApiResponse;
import iuh.fit.fe.dto.FileResponse;
import iuh.fit.fe.service.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@Slf4j
public class FileController {
    FileService fileService;
    @PostMapping("/media/upload")
    ApiResponse<FileResponse> uploadFile(@RequestParam("file") MultipartFile file ) throws IOException {
        return ApiResponse.<FileResponse>builder()
                .result(fileService.uploadFile(file))
                .build();
    }

    @GetMapping("/media/download/{fileName}")
    ResponseEntity<Resource> downloadFile(@PathVariable("fileName") String fileName ) throws IOException {
        var fileData = fileService.downloadFile(fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE,fileData.contentType())
                .body(fileData.resource());
    }
}
