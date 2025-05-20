package it.aredegalli.printer.service.rendering;

import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

@Component
public class StreamStlRenderer {
    private static class Box2D {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        void update(float x, float y) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
        }

        float getWidth() {
            return maxX - minX;
        }

        float getHeight() {
            return maxY - minY;
        }
    }

    /**
     * Renders a top-down preview using two streaming passes:
     * 1) compute bounding box in X,Y
     * 2) draw triangles scaled into the image
     *
     * @param inputSupplier supplies fresh InputStream of the binary STL
     * @param width         output image width
     * @param height        output image height
     */
    public BufferedImage render(Supplier<InputStream> inputSupplier, int width, int height) throws IOException {
        Box2D box;
        // Pass 1: bounding box
        try (DataInputStream dis = new DataInputStream(inputSupplier.get())) {
            dis.skipBytes(80); // header
            int triCount = Integer.reverseBytes(dis.readInt());
            box = new Box2D();
            for (int i = 0; i < triCount; i++) {
                dis.skipBytes(12); // normal
                for (int v = 0; v < 3; v++) {
                    float x = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
                    float y = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
                    dis.skipBytes(4); // z
                    box.update(x, y);
                }
                dis.skipBytes(2); // attr byte count
            }
        }

        float scale = Math.min(width / box.getWidth(), height / box.getHeight());
        float tx = -box.minX * scale;
        float ty = -box.minY * scale;

        // Pass 2: render
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.GRAY);

        try (DataInputStream dis = new DataInputStream(inputSupplier.get())) {
            dis.skipBytes(80);
            int triCount = Integer.reverseBytes(dis.readInt());
            for (int i = 0; i < triCount; i++) {
                dis.skipBytes(12);
                int[] xs = new int[3];
                int[] ys = new int[3];
                for (int v = 0; v < 3; v++) {
                    float x = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
                    float y = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
                    dis.skipBytes(4);
                    xs[v] = Math.round(x * scale + tx);
                    ys[v] = height - Math.round(y * scale + ty);
                }
                dis.skipBytes(2);
                g.fillPolygon(xs, ys, 3);
            }
        }
        g.dispose();
        return img;
    }
}
