package it.aredegalli.printer.service.rendering;

import it.aredegalli.printer.service.storage.StorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PreviewServiceImpl implements PreviewService {
    private final StorageService storage;
    private final JmeRenderingService renderer;

    @Override
    public String previewToBase64(String bucketName, String objectKey, int width, int height) {
        try (InputStream stl = storage.download(bucketName, objectKey);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            BufferedImage img = renderer.render(stl, width, height);
            ImageIO.write(img, "PNG", baos);

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException | EntityNotFoundException e) {
            throw new RuntimeException("Errore generazione preview", e);
        }
    }
}
