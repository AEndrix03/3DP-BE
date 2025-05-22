package it.aredegalli.printer.service.slicing;

import it.aredegalli.printer.dto.slicing.model.ModelDto;
import it.aredegalli.printer.dto.storage.UploadResult;
import it.aredegalli.printer.model.slicing.FileResource;
import it.aredegalli.printer.repository.slicing.FileResourceRepository;
import it.aredegalli.printer.service.rendering.PreviewSTLService;
import it.aredegalli.printer.service.storage.StorageService;
import it.aredegalli.printer.util.PrinterCostants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileResourceServiceImpl implements FileResourceService {

    private final FileResourceRepository repo;
    private final StorageService storage;
    private final PreviewSTLService previewSTLService;

    @Override
    public FileResource upload(MultipartFile file) {
        try {
            UploadResult result = storage.upload(
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    PrinterCostants.PRINTER_MODEL_STORAGE_BUCKET_NAME
            );

            FileResource fr = FileResource.builder()
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .fileHash(result.getHashBytes())
                    .objectKey(result.getObjectKey())
                    .bucketName(PrinterCostants.PRINTER_MODEL_STORAGE_BUCKET_NAME)
                    .uploadedAt(Instant.now())
                    .build();

            return repo.save(fr);

        } catch (IOException e) {
            throw new RuntimeException("Upload fallito", e);
        }
    }

    @Override
    public InputStream download(UUID id) {
        FileResource fr = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File non trovato: " + id));
        return storage.download(fr.getBucketName(), fr.getObjectKey());
    }

    @Override
    public List<ModelDto> getAllModels() {
        return repo.findAll()
                .stream()
                .map(model -> ModelDto.builder()
                        .id(model.getId())
                        .name(model.getFileName())
                        .imagePreview(previewSTLService.previewToBase64(model.getBucketName(), model.getObjectKey(), 200, 200))
                        .build())
                .toList();
    }
}
