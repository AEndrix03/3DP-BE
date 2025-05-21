package it.aredegalli.printer.service.rendering.dd;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

@Service
public class STL2DRenderServiceImpl implements STL2DRenderService {

    private static class Vertex {
        float x, y, z;
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
            this.count++;
        }

        float[] avgNormal() {
            if (count == 0) return new float[]{0, 0, 1};
            float ax = nx / count, ay = ny / count, az = nz / count;
            float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            return new float[]{ax / len, ay / len, az / len};
        }
    }

    private static class Tri {
        Vertex v0, v1, v2;

        Tri(Vertex a, Vertex b, Vertex c) {
            this.v0 = a;
            this.v1 = b;
            this.v2 = c;
            // costruisco normali di faccia
            float ux = b.x - a.x, uy = b.y - a.y, uz = b.z - a.z;
            float vx = c.x - a.x, vy = c.y - a.y, vz = c.z - a.z;
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-6f) len = 1;
            nx /= len;
            ny /= len;
            nz /= len;
            // accumulo sulla mesh
            a.addNormal(nx, ny, nz);
            b.addNormal(nx, ny, nz);
            c.addNormal(nx, ny, nz);
        }
    }

    @Override
    public BufferedImage render(Supplier<InputStream> supplier,
                                int width, int height) throws IOException {
        final int SS = 8;  // super-sampling 8×
        int sw = width * SS, sh = height * SS;

        // 1) carico triangoli e vertici
        Map<String, Vertex> verts = new LinkedHashMap<>();
        List<Tri> tris = new ArrayList<>();
        Box3D box = new Box3D();
        parseAll(supplier, verts, tris, box);

        // 2) preparo frame buffer e z-buffer
        BufferedImage hi = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        float[][] zbuf = new float[sw][sh];
        for (int x = 0; x < sw; x++) Arrays.fill(zbuf[x], Float.POSITIVE_INFINITY);

        Graphics2D g = hi.createGraphics();
        // sfondo bianco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, sw, sh);

        // 3) rasterizzo ogni triangolo con Z-test + Gouraud
        float scale = Math.min(sw / box.widthXY(), sh / box.heightXY());
        float tx = -box.minX * scale, ty = -box.minY * scale;
        for (Tri t : tris) {
            rasterTri(g, zbuf, t, scale, tx, ty, sh);
        }
        g.dispose();

        // 4) down-scale bicubico
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(hi, 0, 0, width, height, null);
        g2.dispose();
        return out;
    }

    private void parseAll(Supplier<InputStream> s,
                          Map<String, Vertex> verts,
                          List<Tri> tris,
                          Box3D box) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(s.get())) {
            bis.mark(1024);
            byte[] hdr = bis.readNBytes(80);
            String head = new String(hdr, StandardCharsets.US_ASCII).trim();
            bis.reset();
            if (head.startsWith("solid") && isAscii(bis)) {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(bis, StandardCharsets.US_ASCII))) {
                    String line;
                    List<Vertex> tmp = new ArrayList<>();
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("vertex")) {
                            String[] p = line.split("\\s+");
                            float x = Float.parseFloat(p[1]),
                                    y = Float.parseFloat(p[2]),
                                    z = Float.parseFloat(p[3]);
                            box.update(x, y, z);
                            String key = x + "," + y + "," + z;
                            tmp.add(verts.computeIfAbsent(key, k -> new Vertex(x, y, z)));
                            if (tmp.size() == 3) {
                                tris.add(new Tri(tmp.get(0), tmp.get(1), tmp.get(2)));
                                tmp.clear();
                            }
                        }
                    }
                }
            } else {
                try (DataInputStream dis = new DataInputStream(bis)) {
                    dis.skipBytes(80);
                    int count = Integer.reverseBytes(dis.readInt());
                    for (int i = 0; i < count; i++) {
                        dis.skipBytes(12);
                        Vertex[] tv = new Vertex[3];
                        for (int v = 0; v < 3; v++) {
                            float x = Float.intBitsToFloat(
                                    Integer.reverseBytes(dis.readInt()));
                            float y = Float.intBitsToFloat(
                                    Integer.reverseBytes(dis.readInt()));
                            float z = Float.intBitsToFloat(
                                    Integer.reverseBytes(dis.readInt()));
                            box.update(x, y, z);
                            String key = x + "," + y + "," + z;
                            tv[v] = verts.computeIfAbsent(key, k -> new Vertex(x, y, z));
                        }
                        tris.add(new Tri(tv[0], tv[1], tv[2]));
                        dis.skipBytes(2);
                    }
                }
            }
        }
    }

    private void rasterTri(Graphics2D g, float[][] zbuf, Tri t,
                           float scale, float tx, float ty, int H) {
        // 1) converti in schermo
        float[] fx = {t.v0.x * scale + tx, t.v1.x * scale + tx, t.v2.x * scale + tx};
        float[] fy = {t.v0.y * scale + ty, t.v1.y * scale + ty, t.v2.y * scale + ty};
        float[] fz = {t.v0.z, t.v1.z, t.v2.z};
        int[] xs = new int[3], ys = new int[3];
        for (int i = 0; i < 3; i++) {
            xs[i] = Math.round(fx[i]);
            ys[i] = H - Math.round(fy[i]);
        }
        // 2) bounding box pixels
        int xMin = clamp(Math.min(xs[0], Math.min(xs[1], xs[2])), 0, zbuf.length - 1);
        int xMax = clamp(Math.max(xs[0], Math.max(xs[1], xs[2])), 0, zbuf.length - 1);
        int yMin = clamp(Math.min(ys[0], Math.min(ys[1], ys[2])), 0, zbuf[0].length - 1);
        int yMax = clamp(Math.max(ys[0], Math.max(ys[1], ys[2])), 0, zbuf[0].length - 1);

        // precalc barycentric denom
        float denom = (fy[1] - fy[2]) * (fx[0] - fx[2]) + (fx[2] - fx[1]) * (fy[0] - fy[2]);
        if (Math.abs(denom) < 1e-6f) return;

        // 3) per-pixel inside check + depth test
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                float px = x + 0.5f, py = H - (y + 0.5f);
                // calcola barycentric
                float w0 = ((fy[1] - fy[2]) * (px - fx[2]) + (fx[2] - fx[1]) * (py - fy[2])) / denom;
                float w1 = ((fy[2] - fy[0]) * (px - fx[2]) + (fx[0] - fx[2]) * (py - fy[2])) / denom;
                float w2 = 1f - w0 - w1;
                if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                // depth interp
                float z = w0 * fz[0] + w1 * fz[1] + w2 * fz[2];
                if (z >= zbuf[x][y]) continue;
                zbuf[x][y] = z;
                // shading per-vertex Gouraud
                float[] n0 = t.v0.avgNormal(),
                        n1 = t.v1.avgNormal(),
                        n2 = t.v2.avgNormal();
                float lix = 0.5f, liy = 0.5f, liz = -1f;
                float llen = (float) Math.sqrt(lix * lix + liy * liy + liz * liz);
                lix /= llen;
                liy /= llen;
                liz /= llen;
                float[] n = {
                        n0[0] * w0 + n1[0] * w1 + n2[0] * w2,
                        n0[1] * w0 + n1[1] * w1 + n2[1] * w2,
                        n0[2] * w0 + n1[2] * w1 + n2[2] * w2
                };
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

    private boolean isAscii(BufferedInputStream bis) throws IOException {
        bis.mark(4096);
        byte[] h = bis.readNBytes(80);
        String head = new String(h, StandardCharsets.US_ASCII).trim();
        bis.reset();
        if (!head.startsWith("solid")) return false;
        BufferedReader r = new BufferedReader(new InputStreamReader(bis, StandardCharsets.US_ASCII));
        String l;
        int cnt = 0;
        while ((l = r.readLine()) != null && cnt++ < 30) {
            if (l.trim().startsWith("facet")) {
                bis.reset();
                return true;
            }
        }
        bis.reset();
        return false;
    }

    private static class Box3D {
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