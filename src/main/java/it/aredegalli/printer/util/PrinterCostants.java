package it.aredegalli.printer.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PrinterCostants {

    public static final String PRINTER_MODEL_STORAGE_BUCKET_NAME = "printer-model";
    public static final String PRINTER_IMAGE_STORAGE_BUCKET_NAME = "printer-image";
    public static final String PRINTER_MODEL_GLB_STORAGE_BUCKET_NAME = "printer-glb-model";
    public static final String PRINTER_SLICING_STORAGE_BUCKET_NAME = "printer-slicing-result";

}
