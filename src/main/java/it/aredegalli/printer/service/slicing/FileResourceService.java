package it.aredegalli.printer.service.slicing;

import it.aredegalli.printer.dto.slicing.FileUploadResponseDto;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface FileResourceService {
    FileUploadResponseDto uploadFile(@NotNull MultipartFile file) throws IOException;

    byte[] downloadFile(@NotNull UUID id);

    List<FileUploadResponseDto> getAllFiles();
}
