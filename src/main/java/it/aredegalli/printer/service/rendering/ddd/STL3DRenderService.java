package it.aredegalli.printer.service.rendering.ddd;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Servizio per il rendering 3D di file STL.
 * Supporta diversi formati di output per garantire massima flessibilità.
 */
public interface STL3DRenderService {

    /**
     * Renderizza un file STL in formato Three.js JSON.
     *
     * @param supplier Fornitore dello stream di input contenente i dati STL
     * @return Stringa contenente il modello in formato JSON per Three.js
     */
    String renderToThreeJS(Supplier<InputStream> supplier);

    /**
     * Renderizza un file STL in formato binario WebGL.
     *
     * @param supplier Fornitore dello stream di input contenente i dati STL
     * @return Array di byte contenente la rappresentazione binaria ottimizzata per WebGL
     */
    byte[] renderToWebGL(Supplier<InputStream> supplier);

    /**
     * Renderizza un file STL come scena 3D in formato glTF.
     * glTF (GL Transmission Format) è un formato standard per i modelli 3D
     * ottimizzato per il web.
     *
     * @param supplier     Fornitore dello stream di input contenente i dati STL
     * @param optimizeMesh Se true, ottimizza la mesh per ridurre la dimensione e migliorare le performance
     * @return Array di byte contenente il modello in formato glTF
     */
    byte[] renderToGLTF(Supplier<InputStream> supplier, boolean optimizeMesh);

    /**
     * Renderizza un file STL come modello 3D pronto per essere visualizzato in browser.
     *
     * @param supplier Fornitore dello stream di input contenente i dati STL
     * @param format   Formato desiderato (es. "threejs", "gltf", "webgl")
     * @return Stringa Base64 del modello renderizzato
     */
    String renderToBase64(Supplier<InputStream> supplier, String format);
}
