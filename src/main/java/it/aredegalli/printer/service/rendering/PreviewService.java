package it.aredegalli.printer.service.rendering;

public interface PreviewService {

    String previewToBase64(String bucketName, String objectKey, int width, int height);
}
