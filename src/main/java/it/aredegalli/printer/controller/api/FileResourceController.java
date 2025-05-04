package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.slicing.FileUploadResponseDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.FileResourceService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
public class FileResourceController {

    private final FileResourceService fileResourceService;
    private final LogService log;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponseDto> upload(@RequestParam("file") MultipartFile file) {
        try {
            log.info("FileResourceController", "Uploading file: " + file.getOriginalFilename());
            return ResponseEntity.ok(fileResourceService.uploadFile(file));
        } catch (Exception e) {
            log.error("FileResourceController", "Upload failed: " + e.getMessage());
            throw new RuntimeException("Upload failed", e);
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable @NotNull UUID id) {
        byte[] content = fileResourceService.downloadFile(id);
        log.info("FileResourceController", "Download requested for file ID: " + id);
        ByteArrayResource resource = new ByteArrayResource(content);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + "\"")
                .contentLength(content.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping
    public ResponseEntity<List<FileUploadResponseDto>> getAllFiles() {
        log.info("FileResourceController", "Listing all uploaded files");
        return ResponseEntity.ok(fileResourceService.getAllFiles());
    }
}
