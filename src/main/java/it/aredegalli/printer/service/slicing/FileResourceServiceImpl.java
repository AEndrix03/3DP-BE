package it.aredegalli.printer.service.slicing;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.slicing.FileUploadResponseDto;
import it.aredegalli.printer.model.slicing.FileResource;
import it.aredegalli.printer.repository.slicing.FileResourceRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.util.HashUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileResourceServiceImpl implements FileResourceService {

    private final FileResourceRepository fileResourceRepository;
    private final LogService log;
    private final HashUtil hashUtil;

    @Override
    public FileUploadResponseDto uploadFile(@NotNull MultipartFile file) throws IOException {
        String checksum = hashUtil.sha256(Arrays.toString(file.getBytes()));

        FileResource fileResource = fileResourceRepository.save(FileResource.builder()
                .fileName(file.getOriginalFilename())
                .content(file.getBytes())
                .uploadedAt(Instant.now())
                .checksum(checksum)
                .build());

        log.info("FileResourceImpl", "File uploaded: " + fileResource.getId());
        return FileUploadResponseDto.builder()
                .id(fileResource.getId())
                .fileName(fileResource.getFileName())
                .checksum(fileResource.getChecksum())
                .build();
    }

    @Override
    public byte[] downloadFile(@NotNull UUID id) {
        FileResource fileResource = fileResourceRepository.findById(id).orElseThrow(() -> new NotFoundException("File not found"));

        log.info("FileResourceImpl", "File downloaded: " + fileResource.getId());
        return fileResource.getContent();
    }

    @Override
    public List<FileUploadResponseDto> getAllFiles() {
        List<FileResource> fileResources = fileResourceRepository.findAll();
        return fileResources.stream()
                .map(fileResource -> FileUploadResponseDto.builder()
                        .id(fileResource.getId())
                        .fileName(fileResource.getFileName())
                        .checksum(fileResource.getChecksum())
                        .build())
                .toList();
    }

}
