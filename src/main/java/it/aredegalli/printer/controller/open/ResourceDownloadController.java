package it.aredegalli.printer.controller.open;

import it.aredegalli.printer.service.resource.FileResourceService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/public/download")
@RequiredArgsConstructor
public class ResourceDownloadController {

    private final FileResourceService fileResourceService;

    @GetMapping
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam("token") @NotNull String token) {
        InputStream inputStream = fileResourceService.download(token);
        StreamingResponseBody responseBody = outputStream -> {
            try (inputStream) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException("Error streaming file", e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + token + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }
}
