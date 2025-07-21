package it.aredegalli.printer.service.storage;

import it.aredegalli.printer.dto.storage.UploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Servizio che gestisce l'upload e il download su storage S3/MinIO,
 * calcolando l'hash SHA-256 on-the-fly e usando chiavi basate su hash.
 */
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;

    @Override
    public UploadResult upload(InputStream data,
                               long size,
                               String contentType,
                               String bucket) {
        byte[] hashBytes;
        String hashHex;
        String tempKey = UUID.randomUUID().toString();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            DigestInputStream dis = new DigestInputStream(data, md);

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos, 8 * 1024);

            Thread pump = getDigestThread(dis, pos);

            PutObjectRequest tempPut = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(tempKey)
                    .contentType(contentType)
                    .contentLength(size)
                    .build();
            s3Client.putObject(tempPut, RequestBody.fromInputStream(pis, size));

            pump.join();
            hashBytes = md.digest();
            StringBuilder sb = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            hashHex = sb.toString();

            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .copySource(bucket + "/" + tempKey)
                    .destinationBucket(bucket)
                    .destinationKey(hashHex)
                    .build();
            s3Client.copyObject(copyReq);

            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(tempKey)
                    .build();
            s3Client.deleteObject(delReq);

        } catch (Exception e) {
            throw new RuntimeException("Errore durante l'upload dello stream", e);
        }

        return new UploadResult(hashHex, hashBytes);
    }

    private static Thread getDigestThread(DigestInputStream dis, PipedOutputStream pos) {
        Thread pump = new Thread(() -> {
            byte[] buffer = new byte[4 * 1024];
            int read;
            try {
                while ((read = dis.read(buffer)) != -1) {
                    pos.write(buffer, 0, read);
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    pos.close();
                } catch (Exception ignored) {
                }
                try {
                    dis.close();
                } catch (Exception ignored) {
                }
            }
        }, "s3-hash-pump");
        pump.start();
        return pump;
    }

    @Override
    public InputStream download(String bucket, String objectKey) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        return s3Client.getObject(getReq);
    }
}