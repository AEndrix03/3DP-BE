package it.aredegalli.printer.service.rendering;

public interface PreviewSTLService {

    String previewToBase64(String bucketName, String objectKey, int width, int height);

    String model3DToBase64(String bucketName, String objectKey, String format);

    String modelToThreeJS(String bucketName, String objectKey);

    byte[] modelToGLTF(String bucketName, String objectKey, boolean optimize);
}
