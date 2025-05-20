package it.aredegalli.printer.service.rendering;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JmeRenderingService extends SimpleApplication {
    private static final Logger LOGGER = Logger.getLogger(JmeRenderingService.class.getName());
    
    private BufferedImage screenshot;
    private CountDownLatch latch;
    private InputStream stlStream;
    private int width, height;
    private String modelName;

    public void init(InputStream stlStream, int width, int height) {
        this.stlStream = stlStream;
        this.width = width;
        this.height = height;
        this.latch = new CountDownLatch(1);
        this.modelName = "model";

        AppSettings settings = new AppSettings(true);
        settings.setWidth(width);
        settings.setHeight(height);
        settings.setAudioRenderer(null);
        settings.setSamples(8);                // 8× MSAA
        settings.setFrameRate(0);              // off-screen non ha FPS
        setSettings(settings);
        setShowSettings(false);
        start(JmeContext.Type.OffscreenSurface);
    }

    @Override
    public void simpleInitApp() {
        try {
            // Configura l'AssetManager per supportare STL
            STLLoaderUtil.configureAssetManager(assetManager);
            
            // Carica il modello STL utilizzando la nuova utilità
            Spatial model = STLLoaderUtil.loadSTLModel(assetManager, stlStream, modelName);
            
            rootNode.attachChild(model);

            // Applica materiale PBR
            model.setMaterial(new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md"));
            
            // Aggiungi luci
            rootNode.addLight(new DirectionalLight(new Vector3f(-1, -2, -3).normalizeLocal(), ColorRGBA.White));
            AmbientLight amb = new AmbientLight(ColorRGBA.White.mult(0.3f));
            rootNode.addLight(amb);

            // Posiziona la camera
            BoundingVolume bv = model.getWorldBound();
            float radius = ((BoundingSphere) bv).getRadius();
            cam.setLocation(new Vector3f(0, 0, radius * 3));
            cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

            // Render e screenshot
            enqueue(() -> {
                try {
                    FrameBuffer fb = viewPort.getOutputFrameBuffer();
                    ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
                    renderer.readFrameBuffer(fb, buf);

                    screenshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Screenshots.convertScreenShot(buf, screenshot);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Errore durante il rendering", e);
                } finally {
                    latch.countDown();
                    stop();
                }
                return null;
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore in simpleInitApp", e);
            latch.countDown();
            stop();
        }
    }

    public BufferedImage render(InputStream stlStream, int width, int height) {
        init(stlStream, width, height);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Interruzione durante il rendering", e);
        }
        return screenshot;
    }
}
