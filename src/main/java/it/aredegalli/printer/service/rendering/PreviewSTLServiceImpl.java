package it.aredegalli.printer.service.rendering;

import it.aredegalli.printer.service.rendering.dd.STL2DRenderServiceImpl;
import it.aredegalli.printer.service.rendering.ddd.STL3DRenderService;
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
    private final STL3DRenderService renderer3D;

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

    /**
     * Genera una rappresentazione 3D del modello STL nel formato specificato.
     *
     * @param bucketName Nome del bucket di storage
     * @param objectKey  Chiave dell'oggetto nel bucket
     * @param format     Formato desiderato (threejs, gltf, webgl)
     * @return Stringa Base64 del modello 3D
     */
    @Override
    public String model3DToBase64(String bucketName, String objectKey, String format) {
        try {
            Supplier<InputStream> supplier = () -> storage.download(bucketName, objectKey);
            return renderer3D.renderToBase64(supplier, format);
        } catch (Exception e) {
            log.error("Errore generazione modello 3D STL: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Genera una rappresentazione 3D del modello STL in formato Three.js JSON.
     *
     * @param bucketName Nome del bucket di storage
     * @param objectKey  Chiave dell'oggetto nel bucket
     * @return JSON per Three.js
     */
    @Override
    public String modelToThreeJS(String bucketName, String objectKey) {
        try {
            Supplier<InputStream> supplier = () -> storage.download(bucketName, objectKey);
            return renderer3D.renderToThreeJS(supplier);
        } catch (Exception e) {
            log.error("Errore generazione modello Three.js: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Genera una rappresentazione 3D del modello STL in formato glTF binario.
     *
     * @param bucketName Nome del bucket di storage
     * @param objectKey  Chiave dell'oggetto nel bucket
     * @param optimize   Se true, ottimizza la mesh per migliorare le performance
     * @return Array di byte contenente il modello in formato glTF
     */
    @Override
    public byte[] modelToGLTF(String bucketName, String objectKey, boolean optimize) {
        try {
            Supplier<InputStream> supplier = () -> storage.download(bucketName, objectKey);
            return renderer3D.renderToGLTF(supplier, optimize);
        } catch (Exception e) {
            log.error("Errore generazione modello glTF: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Genera una rappresentazione 3D del modello STL in formato binario WebGL.
     *
     * @param bucketName Nome del bucket di storage
     * @param objectKey  Chiave dell'oggetto nel bucket
     * @return Array di byte contenente la rappresentazione binaria WebGL
     */
    public byte[] modelToWebGL(String bucketName, String objectKey) {
        try {
            Supplier<InputStream> supplier = () -> storage.download(bucketName, objectKey);
            return renderer3D.renderToWebGL(supplier);
        } catch (Exception e) {
            log.error("Errore generazione modello WebGL: {}", e.getMessage(), e);
            return null;
        }
    }
}
