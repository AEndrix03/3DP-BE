package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.slicing.model.ModelDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.FileResourceService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
public class FileResourceController {

    private final FileResourceService fileResourceService;
    private final LogService log;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UUID> upload(@RequestParam("file") MultipartFile file) {
        try {
            log.info("FileResourceController", "Uploading file: " + file.getOriginalFilename());
            return ResponseEntity.ok(fileResourceService.upload(file).getId());
        } catch (Exception e) {
            log.error("FileResourceController", "Upload failed: " + e.getMessage());
            throw new RuntimeException("Upload failed", e);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam("id") @NotNull UUID id) {
        log.info("FileResourceController", "Download requested for file ID: " + id);
        return _download(fileResourceService.download(id), id);
    }

    @GetMapping("/download/glb")
    public ResponseEntity<StreamingResponseBody> downloadGlb(@RequestParam("id") @NotNull UUID id) {
        log.info("FileResourceController", "GLB Download requested for file ID: " + id);
        return _download(fileResourceService.downloadGlb(id), id);
    }

    @GetMapping
    public ResponseEntity<List<ModelDto>> getAllFiles() {
        log.info("FileResourceController", "Listing all uploaded files");
        return ResponseEntity.ok(fileResourceService.getAllModels());
    }

    private ResponseEntity<StreamingResponseBody> _download(InputStream inputStream, UUID id) {
        StreamingResponseBody responseBody = outputStream -> {
            try (inputStream) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } catch (IOException e) {
                log.error("FileResourceController", "Error streaming file with ID: " + id);
                throw new RuntimeException("Error streaming file", e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }
}
