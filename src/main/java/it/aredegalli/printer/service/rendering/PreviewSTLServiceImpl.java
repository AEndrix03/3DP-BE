package it.aredegalli.printer.service.rendering;

import it.aredegalli.printer.service.rendering.dd.STL2DRenderServiceImpl;
import it.aredegalli.printer.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewSTLServiceImpl implements PreviewSTLService {
    private final StorageService storage;
    private final STL2DRenderServiceImpl renderer2D;

    /**
     * Genera una preview 2D del modello STL come immagine codificata in Base64.
     *
     * @param bucketName Nome del bucket di storage
     * @param objectKey  Chiave dell'oggetto nel bucket
     * @param width      Larghezza dell'immagine di output
     * @param height     Altezza dell'immagine di output
     * @return Stringa Base64 dell'immagine PNG
     */
    @Override
    public String previewToBase64(String bucketName, String objectKey, int width, int height) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Supplier<InputStream> supplier = () -> storage.download(bucketName, objectKey);

            BufferedImage img = renderer2D.render(supplier, width, height);

            ImageIO.write(img, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException | UncheckedIOException e) {
            log.error("Errore generazione preview STL: {}", e.getMessage(), e);
            return null;
        }
    }
}
