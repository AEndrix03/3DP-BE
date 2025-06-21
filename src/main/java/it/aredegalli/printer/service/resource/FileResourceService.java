package it.aredegalli.printer.service.resource;

import it.aredegalli.printer.model.resource.FileResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

public interface FileResourceService {

    FileResource upload(MultipartFile file);

    InputStream download(UUID id);

    InputStream downloadGlb(UUID id);
}
