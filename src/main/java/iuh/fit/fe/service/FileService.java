package iuh.fit.fe.service;

import iuh.fit.fe.dto.FileData;
import iuh.fit.fe.dto.FileResponse;
import iuh.fit.fe.exception.AppException;
import iuh.fit.fe.exception.ErrorCode;
import iuh.fit.fe.mapper.FileMgmtMapper;
import iuh.fit.fe.repository.FileMgmtRepository;
import iuh.fit.fe.repository.FileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
@Service
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class FileService {
    FileRepository fileRepository;
    FileMgmtRepository fileMgmtRepository;
    FileMgmtMapper fileMgmtMapper;
    public FileResponse uploadFile(MultipartFile file) throws IOException {
        // Store file

        var fileInfo = fileRepository.store(file);

        // Create file management info
        var filemgmt = fileMgmtMapper.toFileMgmt(fileInfo);
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        filemgmt.setOwnerId(userId);
        filemgmt =  fileMgmtRepository.save(filemgmt); // Return the appropriate response after uploading the file
        return FileResponse.builder()
                .originalFileName(file.getOriginalFilename())
                .url(fileInfo.getUrl())
                .build();
    }

    public FileData downloadFile(String fileName) throws IOException {
        var fileMgmt = fileMgmtRepository.findById(fileName).orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND));
        var resource = fileRepository.read(fileMgmt);
        return new FileData(fileMgmt.getContentType(),resource);
    }
}
