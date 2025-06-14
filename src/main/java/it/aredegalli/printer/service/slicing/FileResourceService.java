package it.aredegalli.printer.service.slicing;

import it.aredegalli.printer.model.slicing.FileResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

public interface FileResourceService {

    FileResource upload(MultipartFile file);

    InputStream download(UUID id);

    InputStream downloadGlb(UUID id);
}
