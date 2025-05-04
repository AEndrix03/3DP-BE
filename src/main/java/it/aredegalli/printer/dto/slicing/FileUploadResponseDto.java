package it.aredegalli.printer.dto.slicing;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FileUploadResponseDto {
    private UUID id;
    private String fileName;
    private String checksum;
}
