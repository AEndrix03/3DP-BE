package it.aredegalli.printer.service.rendering.ddd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

/**
 * Implementazione del servizio di rendering 3D per file STL.
 * Questa implementazione è ottimizzata per:
 * - Elaborazione parallela delle mesh
 * - Gestione efficiente della memoria
 * - Supporto per vari formati di output
 */
@Slf4j
@Service
public class STL3DRenderServiceImpl implements STL3DRenderService {

    // Costanti per configurazione
    private static final int BUFFER_SIZE = 8192;
    private static final int PARSING_THRESHOLD = 1000; // Soglia per elaborazione parallela

    // Pool di thread per l'elaborazione parallela
    private final ForkJoinPool forkJoinPool;

    /**
     * Costruttore che inizializza il pool di thread per l'elaborazione parallela.
     */
    public STL3DRenderServiceImpl() {
        // Utilizza metà dei core disponibili per non sovraccaricare il sistema
        this.forkJoinPool = new ForkJoinPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    }

    /**
     * Classe per rappresentare un vertice 3D
     */
    private static class Vertex {
        float x, y, z;

        Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex vertex = (Vertex) o;
            return Float.compare(vertex.x, x) == 0 &&
                    Float.compare(vertex.y, y) == 0 &&
                    Float.compare(vertex.z, z) == 0;
        }

        @Override
        public int hashCode() {
            int result = Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            result = 31 * result + Float.floatToIntBits(z);
            return result;
        }
    }

    /**
     * Classe per rappresentare una normale (vettore perpendicolare alla superficie)
     */
    private static class Normal {
        float nx, ny, nz;

        Normal(float nx, float ny, float nz) {
            // Normalizzazione del vettore
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                this.nx = nx / length;
                this.ny = ny / length;
                this.nz = nz / length;
            } else {
                this.nx = 0;
                this.ny = 0;
                this.nz = 1; // Normale di default
            }
        }
    }

    /**
     * Classe per rappresentare una faccia (triangolo)
     */
    private static class Face {
        Vertex[] vertices = new Vertex[3];
        Normal normal;

        Face(Vertex v1, Vertex v2, Vertex v3, float nx, float ny, float nz) {
            vertices[0] = v1;
            vertices[1] = v2;
            vertices[2] = v3;
            normal = new Normal(nx, ny, nz);
        }

        // Calcola la normale della faccia in base ai vertici
        void calculateNormal() {
            Vertex v1 = vertices[0];
            Vertex v2 = vertices[1];
            Vertex v3 = vertices[2];

            // Calcolo dei vettori
            float ux = v2.x - v1.x;
            float uy = v2.y - v1.y;
            float uz = v2.z - v1.z;

            float vx = v3.x - v1.x;
            float vy = v3.y - v1.y;
            float vz = v3.z - v1.z;

            // Prodotto vettoriale (normale)
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;

            normal = new Normal(nx, ny, nz);
        }
    }

    /**
     * Classe per rappresentare il modello 3D completo
     */
    private static class Mesh {
        List<Vertex> vertices = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        Map<String, Integer> vertexMap = new HashMap<>(); // Per ottimizzazione

        // Limiti del modello per normalizzazione
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        void addFace(Face face) {
            faces.add(face);

            // Aggiornamento dei limiti
            for (Vertex v : face.vertices) {
                updateBounds(v);
            }
        }

        void updateBounds(Vertex v) {
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            minZ = Math.min(minZ, v.z);
            maxX = Math.max(maxX, v.x);
            maxY = Math.max(maxY, v.y);
            maxZ = Math.max(maxZ, v.z);
        }

        /**
         * Ottimizza la mesh unificando i vertici duplicati
         */
        void optimize() {
            vertices.clear();
            indices.clear();
            vertexMap.clear();

            for (Face face : faces) {
                for (Vertex v : face.vertices) {
                    String key = v.x + "," + v.y + "," + v.z;
                    Integer index = vertexMap.get(key);

                    if (index == null) {
                        index = vertices.size();
                        vertices.add(v);
                        vertexMap.put(key, index);
                    }

                    indices.add(index);
                }
            }

            log.debug("Mesh ottimizzata: {} vertici, {} indici", vertices.size(), indices.size());
        }

        /**
         * Normalizza la mesh per centrarla e scalandola a dimensioni appropriate
         */
        void normalize() {
            if (vertices.isEmpty()) return;

            float sizeX = maxX - minX;
            float sizeY = maxY - minY;
            float sizeZ = maxZ - minZ;
            float maxSize = Math.max(Math.max(sizeX, sizeY), sizeZ);

            // Fattore di scala per portare il modello a dimensioni unitarie
            float scale = maxSize > 0 ? 2.0f / maxSize : 1.0f;

            // Centro del modello
            float centerX = (minX + maxX) / 2;
            float centerY = (minY + maxY) / 2;
            float centerZ = (minZ + maxZ) / 2;

            // Normalizzazione di tutti i vertici
            for (int i = 0; i < vertices.size(); i++) {
                Vertex v = vertices.get(i);
                v.x = (v.x - centerX) * scale;
                v.y = (v.y - centerY) * scale;
                v.z = (v.z - centerZ) * scale;
                vertices.set(i, v);
            }

            // Aggiornamento dei limiti
            minX = -1.0f;
            maxX = 1.0f;
            minY = -1.0f * (sizeY / maxSize);
            maxY = (sizeY / maxSize);
            minZ = -1.0f * (sizeZ / maxSize);
            maxZ = (sizeZ / maxSize);
        }
    }

    /**
     * Task ricorsivo per il parsing parallelo di blocchi di un file STL binario
     */
    private static class StlParsingTask extends RecursiveTask<List<Face>> {
        private final ByteBuffer buffer;
        private final int startOffset;
        private final int faceCount;
        private final int threshold;

        StlParsingTask(ByteBuffer buffer, int startOffset, int faceCount, int threshold) {
            this.buffer = buffer;
            this.startOffset = startOffset;
            this.faceCount = faceCount;
            this.threshold = threshold;
        }

        @Override
        protected List<Face> compute() {
            if (faceCount <= threshold) {
                return parseSequentially();
            }

            // Divide il task in due sottotask
            int mid = faceCount / 2;
            int midOffset = startOffset + (mid * 50);  // 50 bytes per faccia

            StlParsingTask left = new StlParsingTask(buffer, startOffset, mid, threshold);
            StlParsingTask right = new StlParsingTask(buffer, midOffset, faceCount - mid, threshold);

            left.fork();
            List<Face> rightResult = right.compute();
            List<Face> leftResult = left.join();

            // Combina i risultati
            List<Face> result = new ArrayList<>(leftResult.size() + rightResult.size());
            result.addAll(leftResult);
            result.addAll(rightResult);
            return result;
        }

        private List<Face> parseSequentially() {
            List<Face> faces = new ArrayList<>(faceCount);
            buffer.position(startOffset);

            for (int i = 0; i < faceCount; i++) {
                // Leggi la normale
                float normalX = buffer.getFloat();
                float normalY = buffer.getFloat();
                float normalZ = buffer.getFloat();

                // Leggi i vertici
                Vertex v1 = readVertex(buffer);
                Vertex v2 = readVertex(buffer);
                Vertex v3 = readVertex(buffer);

                // Salta l'attributo uint16
                buffer.position(buffer.position() + 2);

                faces.add(new Face(v1, v2, v3, normalX, normalY, normalZ));
            }

            return faces;
        }

        private Vertex readVertex(ByteBuffer buffer) {
            float x = buffer.getFloat();
            float y = buffer.getFloat();
            float z = buffer.getFloat();
            return new Vertex(x, y, z);
        }
    }

    @Override
    public String renderToThreeJS(Supplier<InputStream> supplier) {
        try {
            Mesh mesh = parseMesh(supplier, true);
            return convertToThreeJS(mesh);
        } catch (IOException e) {
            log.error("Errore durante il rendering STL in formato Three.js", e);
            throw new RuntimeException("Errore di rendering STL", e);
        }
    }

    @Override
    public byte[] renderToWebGL(Supplier<InputStream> supplier) {
        try {
            Mesh mesh = parseMesh(supplier, true);
            return convertToWebGL(mesh);
        } catch (IOException e) {
            log.error("Errore durante il rendering STL in formato WebGL", e);
            throw new RuntimeException("Errore di rendering STL", e);
        }
    }

    @Override
    public byte[] renderToGLTF(Supplier<InputStream> supplier, boolean optimizeMesh) {
        try {
            Mesh mesh = parseMesh(supplier, optimizeMesh);
            return convertToGLTF(mesh);
        } catch (IOException e) {
            log.error("Errore durante il rendering STL in formato glTF", e);
            throw new RuntimeException("Errore di rendering STL", e);
        }
    }

    @Override
    public String renderToBase64(Supplier<InputStream> supplier, String format) {
        try {
            byte[] data;

            switch (format.toLowerCase()) {
                case "threejs":
                    return renderToThreeJS(supplier);
                case "gltf":
                    data = renderToGLTF(supplier, true);
                    break;
                case "webgl":
                    data = renderToWebGL(supplier);
                    break;
                default:
                    throw new IllegalArgumentException("Formato non supportato: " + format);
            }

            return Base64.getEncoder().encodeToString(compressData(data));
        } catch (IOException e) {
            log.error("Errore durante il rendering STL in Base64", e);
            throw new RuntimeException("Errore di rendering STL", e);
        }
    }

    /**
     * Analizza un file STL e costruisce un modello Mesh
     */
    private Mesh parseMesh(Supplier<InputStream> supplier, boolean optimize) throws IOException {
        Mesh mesh = new Mesh();

        try (InputStream inputStream = supplier.get()) {
            // Verifica se lo STL è in formato ASCII o binario
            PushbackInputStream pbis = new PushbackInputStream(inputStream, 6);
            byte[] header = new byte[6];
            int read = pbis.read(header);
            pbis.unread(header, 0, read);

            String headerStr = new String(header, StandardCharsets.US_ASCII);
            if (headerStr.toLowerCase().startsWith("solid")) {
                parseAsciiSTL(pbis, mesh);
            } else {
                parseBinarySTL(pbis, mesh);
            }
        }

        if (optimize) {
            mesh.optimize();
            mesh.normalize();
        }

        return mesh;
    }

    /**
     * Analizza un file STL in formato ASCII
     */
    private void parseAsciiSTL(InputStream inputStream, Mesh mesh) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.US_ASCII))) {
            String line;
            float[] normal = new float[3];
            List<Vertex> vertices = new ArrayList<>(3);

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("facet normal ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 5) {
                        normal[0] = Float.parseFloat(parts[2]);
                        normal[1] = Float.parseFloat(parts[3]);
                        normal[2] = Float.parseFloat(parts[4]);
                    }
                } else if (line.startsWith("vertex ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);
                        float z = Float.parseFloat(parts[3]);
                        vertices.add(new Vertex(x, y, z));
                    }
                } else if (line.startsWith("endfacet")) {
                    if (vertices.size() == 3) {
                        mesh.addFace(new Face(
                                vertices.get(0),
                                vertices.get(1),
                                vertices.get(2),
                                normal[0], normal[1], normal[2]
                        ));
                    }
                    vertices.clear();
                }
            }
        }
    }

    /**
     * Analizza un file STL in formato binario utilizzando NIO per prestazioni ottimali
     */
    private void parseBinarySTL(InputStream inputStream, Mesh mesh) throws IOException {
        // Utilizzo di NIO per un I/O efficiente
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        ByteBuffer headerBuffer = ByteBuffer.allocate(84);  // Header da 80 byte + 4 byte per il numero di facce
        headerBuffer.order(ByteOrder.LITTLE_ENDIAN);

        channel.read(headerBuffer);
        headerBuffer.flip();

        // Salta l'header di 80 byte
        headerBuffer.position(80);

        // Leggi il numero di facce
        int faceCount = headerBuffer.getInt();
        log.debug("Parsing STL binario con {} facce", faceCount);

        // Per file molto grandi, usa il parsing parallelo in modo incrementale
        if (faceCount > 100000) {
            parseStlIncrementally(channel, mesh, faceCount);
        } else {
            // Per file più piccoli, leggi tutto in un buffer
            ByteBuffer dataBuffer = ByteBuffer.allocate(faceCount * 50);  // 50 byte per faccia
            dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
            channel.read(dataBuffer);
            dataBuffer.flip();

            // Esegui il parsing parallelo delle facce
            List<Face> faces = forkJoinPool.invoke(
                    new StlParsingTask(dataBuffer, 0, faceCount, PARSING_THRESHOLD));
            for (Face face : faces) {
                mesh.addFace(face);
            }
        }
    }

    /**
     * Analizza in modo incrementale un file STL binario grande
     * Questo approccio divide il file in blocchi gestibili e li elabora in parallelo
     */
    private void parseStlIncrementally(ReadableByteChannel channel, Mesh mesh, int totalFaces) throws IOException {
        final int BLOCK_SIZE = 10000;  // Numero di facce per blocco
        final int BYTES_PER_FACE = 50;  // 50 byte per faccia

        for (int offset = 0; offset < totalFaces; offset += BLOCK_SIZE) {
            int facesToProcess = Math.min(BLOCK_SIZE, totalFaces - offset);
            int bytesToRead = facesToProcess * BYTES_PER_FACE;

            ByteBuffer buffer = ByteBuffer.allocate(bytesToRead);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            channel.read(buffer);
            buffer.flip();

            // Elabora questo blocco di facce in parallelo
            List<Face> faces = forkJoinPool.invoke(
                    new StlParsingTask(buffer, 0, facesToProcess, PARSING_THRESHOLD));
            for (Face face : faces) {
                mesh.addFace(face);
            }
        }
    }

    /**
     * Comprime i dati in formato GZIP per ridurre la dimensione
     */
    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(data);
        }
        return byteStream.toByteArray();
    }

    /**
     * Converte la mesh in formato Three.js JSON
     */
    private String convertToThreeJS(Mesh mesh) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"metadata\": {\n");
        json.append("    \"version\": 4.5,\n");
        json.append("    \"type\": \"Object\",\n");
        json.append("    \"generator\": \"STL3DRenderService\"\n");
        json.append("  },\n");
        json.append("  \"geometries\": [{\n");
        json.append("    \"uuid\": \"").append(UUID.randomUUID()).append("\",\n");
        json.append("    \"type\": \"BufferGeometry\",\n");

        // Vertici
        json.append("    \"data\": {\n");
        json.append("      \"attributes\": {\n");

        // Posizioni
        json.append("        \"position\": {\n");
        json.append("          \"itemSize\": 3,\n");
        json.append("          \"type\": \"Float32Array\",\n");
        json.append("          \"array\": [");

        boolean first = true;
        for (Vertex v : mesh.vertices) {
            if (!first) json.append(",");
            first = false;
            json.append(v.x).append(",").append(v.y).append(",").append(v.z);
        }
        json.append("]\n");
        json.append("        },\n");

        // Normali
        json.append("        \"normal\": {\n");
        json.append("          \"itemSize\": 3,\n");
        json.append("          \"type\": \"Float32Array\",\n");
        json.append("          \"array\": [");

        // Calcola le normali per vertice (media delle normali delle facce adiacenti)
        Map<Integer, List<Normal>> vertexNormals = new HashMap<>();

        // Raccolgo le normali di faccia per ogni vertice
        for (int i = 0; i < mesh.indices.size(); i += 3) {
            Face face = mesh.faces.get(i / 3);

            for (int j = 0; j < 3; j++) {
                int vertexIndex = mesh.indices.get(i + j);
                vertexNormals.computeIfAbsent(vertexIndex, k -> new ArrayList<>())
                        .add(face.normal);
            }
        }

        // Calcolo la media delle normali per ogni vertice
        first = true;
        for (int i = 0; i < mesh.vertices.size(); i++) {
            if (!first) json.append(",");
            first = false;

            List<Normal> normals = vertexNormals.getOrDefault(i, Collections.emptyList());
            if (normals.isEmpty()) {
                json.append("0,1,0"); // Normale di default
            } else {
                float nx = 0, ny = 0, nz = 0;
                for (Normal n : normals) {
                    nx += n.nx;
                    ny += n.ny;
                    nz += n.nz;
                }
                float len = normals.size();
                json.append(nx / len).append(",").append(ny / len).append(",").append(nz / len);
            }
        }
        json.append("]\n");
        json.append("        }\n");
        json.append("      },\n");

        // Indici
        json.append("      \"index\": {\n");
        json.append("        \"type\": \"Uint32Array\",\n");
        json.append("        \"array\": [");

        first = true;
        for (Integer idx : mesh.indices) {
            if (!first) json.append(",");
            first = false;
            json.append(idx);
        }
        json.append("]\n");
        json.append("      }\n");
        json.append("    }\n");
        json.append("  }],\n");

        // Materiali
        json.append("  \"materials\": [{\n");
        json.append("    \"uuid\": \"").append(UUID.randomUUID()).append("\",\n");
        json.append("    \"type\": \"MeshStandardMaterial\",\n");
        json.append("    \"color\": 16777215,\n");
        json.append("    \"roughness\": 0.5,\n");
        json.append("    \"metalness\": 0.5,\n");
        json.append("    \"emissive\": 0,\n");
        json.append("    \"side\": 2\n");
        json.append("  }],\n");

        // Mesh finale
        String geomUuid = UUID.randomUUID().toString();
        String matUuid = UUID.randomUUID().toString();

        json.append("  \"object\": {\n");
        json.append("    \"uuid\": \"").append(UUID.randomUUID()).append("\",\n");
        json.append("    \"type\": \"Mesh\",\n");
        json.append("    \"geometry\": \"").append(geomUuid).append("\",\n");
        json.append("    \"material\": \"").append(matUuid).append("\"\n");
        json.append("  }\n");
        json.append("}");

        return json.toString();
    }

    /**
     * Converte la mesh in un buffer binario WebGL
     */
    private byte[] convertToWebGL(Mesh mesh) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            DataOutputStream dos = new DataOutputStream(baos);

            // Scrivi il numero di vertici
            dos.writeInt(mesh.vertices.size());

            // Scrivi i vertici
            for (Vertex v : mesh.vertices) {
                dos.writeFloat(v.x);
                dos.writeFloat(v.y);
                dos.writeFloat(v.z);
            }

            // Scrivi il numero di indici
            dos.writeInt(mesh.indices.size());

            // Scrivi gli indici
            for (Integer idx : mesh.indices) {
                dos.writeInt(idx);
            }

            dos.flush();
            return baos.toByteArray();
        }
    }

    /**
     * Converte la mesh in formato glTF (versione binaria .glb)
     */
    private byte[] convertToGLTF(Mesh mesh) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            DataOutputStream dos = new DataOutputStream(baos);

            // Preparazione del buffer binario
            ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();
            DataOutputStream binaryStream = new DataOutputStream(binaryBuffer);

            // Scrivi vertici nel buffer
            for (Vertex v : mesh.vertices) {
                binaryStream.writeFloat(v.x);
                binaryStream.writeFloat(v.y);
                binaryStream.writeFloat(v.z);
            }

            // Calcola offset del buffer e padding
            int verticesBufferLength = mesh.vertices.size() * 3 * 4; // 3 float per vertice, 4 byte per float
            int padding = (4 - (verticesBufferLength % 4)) % 4;
            for (int i = 0; i < padding; i++) {
                binaryStream.write(0);
            }

            // Scrivi indici dopo il padding
            int indicesBufferOffset = verticesBufferLength + padding;
            for (Integer idx : mesh.indices) {
                binaryStream.writeShort(idx.shortValue());
            }

            // Calcola padding per indici
            int indicesBufferLength = mesh.indices.size() * 2; // 2 byte per indice (uint16)
            int indicesPadding = (4 - (indicesBufferLength % 4)) % 4;
            for (int i = 0; i < indicesPadding; i++) {
                binaryStream.write(0);
            }

            // Calcola offset per normali
            int normalsBufferOffset = indicesBufferOffset + indicesBufferLength + indicesPadding;

            // Calcola e scrivi normali
            Map<Integer, float[]> vertexNormals = calculateVertexNormals(mesh);
            for (int i = 0; i < mesh.vertices.size(); i++) {
                float[] normal = vertexNormals.getOrDefault(i, new float[]{0, 1, 0});
                binaryStream.writeFloat(normal[0]);
                binaryStream.writeFloat(normal[1]);
                binaryStream.writeFloat(normal[2]);
            }

            binaryStream.flush();
            byte[] binaryData = binaryBuffer.toByteArray();

            // Costruisci il JSON glTF
            String gltfJson = buildGLTFJson(mesh, binaryData.length, indicesBufferOffset, normalsBufferOffset);
            byte[] jsonData = gltfJson.getBytes(StandardCharsets.UTF_8);

            // Padding per il JSON per allineamento
            int jsonPadding = (4 - (jsonData.length % 4)) % 4;

            // Scrivi header glTF
            dos.writeInt(0x46546C67);  // "glTF" magic
            dos.writeInt(2);           // versione 2

            // Lunghezza totale
            int totalLength = 12 + 8 + jsonData.length + jsonPadding + 8 + binaryData.length;
            dos.writeInt(totalLength);

            // Chunk JSON
            dos.writeInt(jsonData.length + jsonPadding);
            dos.writeInt(0x4E4F534A);  // "JSON" type
            dos.write(jsonData);

            // Padding del JSON
            for (int i = 0; i < jsonPadding; i++) {
                dos.write(0);
            }

            // Chunk BIN
            dos.writeInt(binaryData.length);
            dos.writeInt(0x004E4942);  // "BIN\0" type
            dos.write(binaryData);

            dos.flush();
            return baos.toByteArray();
        }
    }


    /**
     * Calcola le normali per vertice basate sulle facce adiacenti
     */
    private Map<Integer, float[]> calculateVertexNormals(Mesh mesh) {
        Map<Integer, List<Normal>> vertexNormalsList = new HashMap<>();
        Map<Integer, float[]> vertexNormals = new HashMap<>();

        // Raccogliamo le normali delle facce per ogni vertice
        for (int i = 0; i < mesh.indices.size(); i += 3) {
            Face face = mesh.faces.get(i / 3);

            for (int j = 0; j < 3; j++) {
                int vertexIndex = mesh.indices.get(i + j);
                vertexNormalsList.computeIfAbsent(vertexIndex, k -> new ArrayList<>())
                        .add(face.normal);
            }
        }

        // Calcoliamo la media delle normali per ogni vertice
        for (Map.Entry<Integer, List<Normal>> entry : vertexNormalsList.entrySet()) {
            int vertexIndex = entry.getKey();
            List<Normal> normals = entry.getValue();

            if (normals.isEmpty()) {
                vertexNormals.put(vertexIndex, new float[]{0, 1, 0}); // Normale di default
            } else {
                float nx = 0, ny = 0, nz = 0;
                for (Normal n : normals) {
                    nx += n.nx;
                    ny += n.ny;
                    nz += n.nz;
                }

                float len = normals.size();
                float[] avgNormal = new float[]{nx / len, ny / len, nz / len};

                // Normalizzazione
                float normalLength = (float) Math.sqrt(avgNormal[0] * avgNormal[0] +
                        avgNormal[1] * avgNormal[1] +
                        avgNormal[2] * avgNormal[2]);
                if (normalLength > 0) {
                    avgNormal[0] /= normalLength;
                    avgNormal[1] /= normalLength;
                    avgNormal[2] /= normalLength;
                }

                vertexNormals.put(vertexIndex, avgNormal);
            }
        }

        return vertexNormals;
    }

    /**
     * Costruisce il JSON completo per il formato glTF
     */
    private String buildGLTFJson(Mesh mesh, int binaryLength, int indicesBufferOffset, int normalsBufferOffset) {

        String json = "{\n" +
                "  \"asset\": {\n" +
                "    \"version\": \"2.0\",\n" +
                "    \"generator\": \"STL3DRenderService\"\n" +
                "  },\n" +

                // Buffer che contiene tutti i dati binari
                "  \"buffers\": [{\n" +
                "    \"byteLength\": " + binaryLength + "\n" +
                "  }],\n" +

                // BufferViews (viste sul buffer per vertici, indici e normali)
                "  \"bufferViews\": [\n" +

                // BufferView per vertici
                "    {\n" +
                "      \"buffer\": 0,\n" +
                "      \"byteOffset\": 0,\n" +
                "      \"byteLength\": " + mesh.vertices.size() * 3 * 4 + ",\n" + // 3 float per vertice, 4 byte per float
                "      \"target\": 34962\n" + // ARRAY_BUFFER
                "    },\n" +

                // BufferView per indici
                "    {\n" +
                "      \"buffer\": 0,\n" +
                "      \"byteOffset\": " + indicesBufferOffset + ",\n" +
                "      \"byteLength\": " + mesh.indices.size() * 2 + ",\n" + // 2 byte per indice (UNSIGNED_SHORT)
                "      \"target\": 34963\n" + // ELEMENT_ARRAY_BUFFER
                "    },\n" +

                // BufferView per normali
                "    {\n" +
                "      \"buffer\": 0,\n" +
                "      \"byteOffset\": " + normalsBufferOffset + ",\n" +
                "      \"byteLength\": " + mesh.vertices.size() * 3 * 4 + ",\n" + // 3 float per normale, 4 byte per float
                "      \"target\": 34962\n" + // ARRAY_BUFFER
                "    }\n" +
                "  ],\n" +

                // Accessors (descrittori per le tipologie di dati)
                "  \"accessors\": [\n" +

                // Accessor per posizioni dei vertici
                "    {\n" +
                "      \"bufferView\": 0,\n" +
                "      \"byteOffset\": 0,\n" +
                "      \"componentType\": 5126,\n" + // FLOAT
                "      \"count\": " + mesh.vertices.size() + ",\n" +
                "      \"type\": \"VEC3\",\n" +

                // Calcola min e max per il bounding box
                "      \"min\": [" + mesh.minX + ", " + mesh.minY + ", " + mesh.minZ + "],\n" +
                "      \"max\": [" + mesh.maxX + ", " + mesh.maxY + ", " + mesh.maxZ + "]\n" +
                "    },\n" +

                // Accessor per normali
                "    {\n" +
                "      \"bufferView\": 2,\n" +
                "      \"byteOffset\": 0,\n" +
                "      \"componentType\": 5126,\n" + // FLOAT
                "      \"count\": " + mesh.vertices.size() + ",\n" +
                "      \"type\": \"VEC3\"\n" +
                "    },\n" +

                // Accessor per indici
                "    {\n" +
                "      \"bufferView\": 1,\n" +
                "      \"byteOffset\": 0,\n" +
                "      \"componentType\": 5123,\n" + // UNSIGNED_SHORT
                "      \"count\": " + mesh.indices.size() + ",\n" +
                "      \"type\": \"SCALAR\"\n" +
                "    }\n" +
                "  ],\n" +

                // Materiali
                "  \"materials\": [\n" +
                "    {\n" +
                "      \"pbrMetallicRoughness\": {\n" +
                "        \"baseColorFactor\": [0.8, 0.8, 0.8, 1.0],\n" +
                "        \"metallicFactor\": 0.5,\n" +
                "        \"roughnessFactor\": 0.5\n" +
                "      },\n" +
                "      \"doubleSided\": true\n" +
                "    }\n" +
                "  ],\n" +

                // Mesh
                "  \"meshes\": [\n" +
                "    {\n" +
                "      \"primitives\": [\n" +
                "        {\n" +
                "          \"attributes\": {\n" +
                "            \"POSITION\": 0,\n" +
                "            \"NORMAL\": 1\n" +
                "          },\n" +
                "          \"indices\": 2,\n" +
                "          \"material\": 0,\n" +
                "          \"mode\": 4\n" + // TRIANGLES
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +

                // Nodi
                "  \"nodes\": [\n" +
                "    {\n" +
                "      \"mesh\": 0\n" +
                "    }\n" +
                "  ],\n" +

                // Scene
                "  \"scenes\": [\n" +
                "    {\n" +
                "      \"nodes\": [0]\n" +
                "    }\n" +
                "  ],\n" +

                // Scena principale
                "  \"scene\": 0\n" +
                "}";

        return json;
    }
}