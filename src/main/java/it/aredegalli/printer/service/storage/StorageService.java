package it.aredegalli.printer.service.storage;

import it.aredegalli.printer.dto.storage.UploadResult;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    /**
     * Carica lo stream sul bucket, calcola l’hash, e restituisce la chiave e l’hash.
     */
    UploadResult upload(InputStream data,
                        long size,
                        String contentType,
                        String bucket) throws IOException;

    /**
     * Ritorna uno InputStream dei dati salvati con quella chiave.
     */
    InputStream download(String bucket, String objectKey);
}
