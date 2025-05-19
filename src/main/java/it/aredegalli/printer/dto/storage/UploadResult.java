package it.aredegalli.printer.dto.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResult {
    private final String objectKey;
    private final byte[] hashBytes;
}
