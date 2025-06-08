package it.aredegalli.printer.service.glb;

import it.aredegalli.printer.model.glb.StlGlbConvert;
import it.aredegalli.printer.repository.glb.StlGlbConvertRepository;
import it.aredegalli.printer.service.glb.stl2glb.Stl2GlbService;
import it.aredegalli.printer.service.storage.StorageService;
import it.aredegalli.printer.util.PrinterCostants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StlGlbConvertServiceImpl implements StlGlbConvertService {

    private final StlGlbConvertRepository stlGlbConvertRepository;
    private final Stl2GlbService stl2GlbService;
    private final StorageService storageService;

    @Override
    public String getGlbHashByObjectKey(String objectKey) {
        var glbHash = stlGlbConvertRepository.findById(objectKey).orElse(null);
        return glbHash != null ? glbHash.getGlbHash() : null;
    }

    @Override
    public String convertStlToGlb(String objectKey) {
        log.info("Converting STL to GLB for object key: {}", objectKey);
        String glbHash = stl2GlbService.convertStlToGlb(objectKey);

        if (glbHash != null) {
            stlGlbConvertRepository.save(
                    StlGlbConvert.builder()
                            .stlHash(objectKey)
                            .glbHash(glbHash)
                            .build()
            );
            log.info("Conversion successful, GLB hash: {}", glbHash);
            return glbHash;
        } else {
            log.error("Conversion failed for object key: {}", objectKey);
            return null;
        }
    }

    @Override
    public InputStream downloadGlbByObjectkey(String objectKey) {
        log.info("Downloading GLB for object key: {}", objectKey);

        String glbHash = getGlbHashByObjectKey(objectKey);
        if (glbHash == null) {
            log.info("No GLB found for object key: {}. Start convertions... ", objectKey);
            glbHash = convertStlToGlb(objectKey);
        }

        log.info("GLB hash: {}", glbHash);

        return storageService.download(
                PrinterCostants.PRINTER_MODEL_GLB_STORAGE_BUCKET_NAME,
                glbHash
        );
    }
}
