package it.aredegalli.printer.service.glb;

import java.io.InputStream;

public interface StlGlbConvertService {

    String getGlbHashByObjectKey(String objectKey);

    String convertStlToGlb(String objectKey);

    InputStream downloadGlbByObjectkey(String objectKey);
}
