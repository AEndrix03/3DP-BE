package it.aredegalli.printer.service.rendering;

import com.github.stephengold.wrench.LwjglAssetKey;
import com.github.stephengold.wrench.LwjglAssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Spatial;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilità per caricare i file STL utilizzando MonkeyWrench
 */
@Component
public class STLLoaderUtil {

    private static final Logger LOGGER = Logger.getLogger(STLLoaderUtil.class.getName());

    /**
     * Configura l'AssetManager per supportare i file STL
     *
     * @param assetManager AssetManager da configurare
     */
    public static void configureAssetManager(AssetManager assetManager) {
        // Registra il loader MonkeyWrench per vari formati, incluso STL
        assetManager.registerLoader(LwjglAssetLoader.class, "stl");
        LOGGER.info("Registrato loader per file STL");
    }

    /**
     * Carica un modello STL da un InputStream
     *
     * @param assetManager AssetManager configurato
     * @param inputStream  InputStream contenente i dati STL
     * @param modelName    Nome da dare al modello
     * @return Spatial creato dal file STL
     */
    public static Spatial loadSTLModel(AssetManager assetManager, InputStream inputStream, String modelName) throws IOException {
        // Crea una directory temporanea per il file
        Path tempDir = Files.createTempDirectory("stl-loader");
        Path tempFile = tempDir.resolve(modelName + ".stl");

        // Copia l'InputStream nel file temporaneo
        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        // Crea una chiave di asset che supporta il debug
        LwjglAssetKey key = new LwjglAssetKey(tempFile.toAbsolutePath().toString());
        key.setVerboseLogging(true);

        try {
            // Carica il modello
            Spatial model = assetManager.loadModel(key);
            LOGGER.info("Modello STL caricato con successo: " + modelName);
            return model;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore nel caricamento del modello STL: " + modelName, e);
            throw e;
        } finally {
            // Pulisci i file temporanei
            try {
                Files.deleteIfExists(tempFile);
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Impossibile eliminare i file temporanei", e);
            }
        }
    }
}