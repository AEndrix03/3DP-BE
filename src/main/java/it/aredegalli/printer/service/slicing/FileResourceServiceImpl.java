package it.aredegalli.printer.service.slicing;

import it.aredegalli.printer.dto.storage.UploadResult;
import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.FileResource;
import it.aredegalli.printer.repository.model.ModelRepository;
import it.aredegalli.printer.repository.slicing.FileResourceRepository;
import it.aredegalli.printer.service.glb.StlGlbConvertService;
import it.aredegalli.printer.service.storage.StorageService;
import it.aredegalli.printer.util.PrinterCostants;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileResourceServiceImpl implements FileResourceService {

    private final FileResourceRepository repo;
    private final StorageService storage;
    private final StlGlbConvertService stlGlbConvertService;
    private final ModelRepository modelRepository;

    @Override
    @Transactional
    public FileResource upload(MultipartFile file) {
        try {
            UploadResult result = storage.upload(
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    PrinterCostants.PRINTER_MODEL_STORAGE_BUCKET_NAME
            );

            stlGlbConvertService.convertStlToGlb(result.getObjectKey());

            FileResource fr = FileResource.builder()
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .fileHash(result.getHashBytes())
                    .objectKey(result.getObjectKey())
                    .bucketName(PrinterCostants.PRINTER_MODEL_STORAGE_BUCKET_NAME)
                    .uploadedAt(Instant.now())
                    .build();

            fr = repo.save(fr);

            Model model = Model.builder()
                    .name(fr.getFileName())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .fileResource(fr)
                    .build();

            this.modelRepository.save(model);

            return fr;
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
    public InputStream downloadGlb(UUID id) {
        FileResource fr = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File non trovato: " + id));
        return stlGlbConvertService.downloadGlbByObjectkey(fr.getObjectKey());
    }

}
