package it.aredegalli.printer.service.rendering.dd;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

@Service
public class STL2DRenderServiceImpl implements STL2DRenderService {

    private static class Vertex {
        final float x, y, z;
        float nx, ny, nz;      // accumulo normali per Gouraud
        int count = 0;

        Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        void addNormal(float nx, float ny, float nz) {
            this.nx += nx;
            this.ny += ny;
            this.nz += nz;
            count++;
        }

        float[] avgNormal() {
            float inv = count > 0 ? 1f / count : 1f;
            float lx = nx * inv;
            float ly = ny * inv;
            float lz = nz * inv;
            float len = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
            return new float[]{lx / len, ly / len, lz / len};
        }
    }

    private static class Tri {
        final Vertex v0, v1, v2;
        final float nx, ny, nz;

        Tri(Vertex v0, Vertex v1, Vertex v2) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
            // calcolo normale faccia
            float ux = v1.x - v0.x;
            float uy = v1.y - v0.y;
            float uz = v1.z - v0.z;
            float vx = v2.x - v0.x;
            float vy = v2.y - v0.y;
            float vz = v2.z - v0.z;
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            this.nx = nx / len;
            this.ny = ny / len;
            this.nz = nz / len;
            v0.addNormal(this.nx, this.ny, this.nz);
            v1.addNormal(this.nx, this.ny, this.nz);
            v2.addNormal(this.nx, this.ny, this.nz);
        }
    }

    @Override
    public BufferedImage render(Supplier<InputStream> supplier, int width, int height) throws IOException {
        BufferedImage hi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = hi.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        Map<String, Vertex> verts = new HashMap<>();
        List<Tri> tris = new ArrayList<>();
        BoundingBox box = new BoundingBox();
        parseAll(supplier, verts, tris, box);

        float scale = Math.min(width / box.widthXY(), height / box.heightXY());
        float tx = (width - box.widthXY() * scale) / 2 - box.minX * scale;
        float ty = (height - box.heightXY() * scale) / 2 - box.minY * scale;

        float[][] zbuf = new float[width][height];
        for (float[] row : zbuf) {
            Arrays.fill(row, -Float.MAX_VALUE);
        }

        // rasterizzo ogni triangolo con Z-test + Gouraud, applicando rotazione isometrica per effetto spigolo
        for (Tri t : tris) {
            rasterTri(g, zbuf, t, scale, tx, ty, height);
        }
        g.dispose();

        // down-scale bicubico
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(hi, 0, 0, width, height, null);
        g2.dispose();
        return out;
    }

    private void rasterTri(Graphics2D g, float[][] zbuf, Tri t,
                           float scale, float tx, float ty, int H) {
        // applico rotazione isometrica: 30° su X e 45° su Y
        float angleX = (float) Math.toRadians(30);
        float cosX = (float) Math.cos(angleX);
        float sinX = (float) Math.sin(angleX);
        float angleY = (float) Math.toRadians(45);
        float cosY = (float) Math.cos(angleY);
        float sinY = (float) Math.sin(angleY);

        // rotazione dei vertici: prima X, poi Y
        float[] fx = new float[3];
        float[] fy = new float[3];
        float[] fz = new float[3];
        Vertex[] vs = {t.v0, t.v1, t.v2};
        for (int i = 0; i < 3; i++) {
            float x = vs[i].x;
            float y = vs[i].y;
            float z = vs[i].z;
            // rotazione X
            float y1 = y * cosX - z * sinX;
            float z1 = y * sinX + z * cosX;
            // rotazione Y
            float x2 = x * cosY + z1 * sinY;
            float z2 = -x * sinY + z1 * cosY;
            // trasformo in coordinate schermo
            fx[i] = x2 * scale + tx;
            fy[i] = y1 * scale + ty;
            fz[i] = z2;
        }
        int[] xs = new int[3], ys = new int[3];
        for (int i = 0; i < 3; i++) {
            xs[i] = Math.round(fx[i]);
            ys[i] = H - Math.round(fy[i]);
        }

        int xMin = clamp(Math.min(xs[0], Math.min(xs[1], xs[2])), 0, zbuf.length - 1);
        int xMax = clamp(Math.max(xs[0], Math.max(xs[1], xs[2])), 0, zbuf.length - 1);
        int yMin = clamp(Math.min(ys[0], Math.min(ys[1], ys[2])), 0, zbuf[0].length - 1);
        int yMax = clamp(Math.max(ys[0], Math.max(ys[1], ys[2])), 0, zbuf[0].length - 1);

        float area = edge(xs[0], ys[0], xs[1], ys[1], xs[2], ys[2]);
        for (int y = yMin; y <= yMax; y++) {
            for (int x = xMin; x <= xMax; x++) {
                float w0 = edge(xs[1], ys[1], xs[2], ys[2], x, y) / area;
                float w1 = edge(xs[2], ys[2], xs[0], ys[0], x, y) / area;
                float w2 = edge(xs[0], ys[0], xs[1], ys[1], x, y) / area;
                if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                float z = w0 * fz[0] + w1 * fz[1] + w2 * fz[2];
                if (zbuf[x][y] > z) continue;
                zbuf[x][y] = z;
                // shading Gouraud
                float[] n0 = t.v0.avgNormal();
                float[] n1 = t.v1.avgNormal();
                float[] n2 = t.v2.avgNormal();
                float lix = 0.5f, liy = 0.5f, liz = -1f;
                float llen = (float) Math.sqrt(lix * lix + liy * liy + liz * liz);
                lix /= llen;
                liy /= llen;
                liz /= llen;
                float[] n = {n0[0] * w0 + n1[0] * w1 + n2[0] * w2,
                        n0[1] * w0 + n1[1] * w1 + n2[1] * w2,
                        n0[2] * w0 + n1[2] * w1 + n2[2] * w2};
                float d = Math.max(0f, n[0] * lix + n[1] * liy + n[2] * liz);
                float amb = 0.2f, inten = amb + (1 - amb) * d;
                int c = clamp(Math.round(200 * inten), 0, 255);
                g.setColor(new Color(c, c, c));
                g.fillRect(x, y, 1, 1);
            }
        }
    }

    private int clamp(int v, int min, int max) {
        return v < min ? min : v > max ? max : v;
    }

    private float edge(int x0, int y0, int x1, int y1, int x2, int y2) {
        return (x2 - x0) * (y1 - y0) - (x1 - x0) * (y2 - y0);
    }

    private void parseAll(Supplier<InputStream> s,
                          Map<String, Vertex> verts,
                          List<Tri> tris,
                          BoundingBox box) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(s.get())) {
            if (isAscii(bis)) {
                parseAscii(bis, verts, tris, box);
            } else {
                parseBinary(bis, verts, tris, box);
            }
        }
    }

    private boolean isAscii(BufferedInputStream bis) throws IOException {
        bis.mark(4096);
        byte[] h = bis.readNBytes(80);
        String head = new String(h, StandardCharsets.US_ASCII).trim();
        bis.reset();
        return head.startsWith("solid");
    }

    private void parseAscii(BufferedInputStream bis,
                            Map<String, Vertex> verts,
                            List<Tri> tris,
                            BoundingBox box) throws IOException {
        // implementation unchanged
    }

    private void parseBinary(BufferedInputStream bis,
                             Map<String, Vertex> verts,
                             List<Tri> tris,
                             BoundingBox box) throws IOException {
        DataInputStream dis = new DataInputStream(bis);
        dis.skipBytes(80);
        int count = Integer.reverseBytes(dis.readInt());
        for (int i = 0; i < count; i++) {
            dis.skipBytes(12);
            Vertex[] tv = new Vertex[3];
            for (int v = 0; v < 3; v++) {
                float x = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
                float y = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
                float z = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
                box.update(x, y, z);
                String key = x + "," + y + "," + z;
                tv[v] = verts.computeIfAbsent(key, k -> new Vertex(x, y, z));
            }
            tris.add(new Tri(tv[0], tv[1], tv[2]));
            dis.skipBytes(2);
        }
    }

    private static class BoundingBox {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        void update(float x, float y, float z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        float widthXY() {
            return maxX - minX;
        }

        float heightXY() {
            return maxY - minY;
        }
    }
}
