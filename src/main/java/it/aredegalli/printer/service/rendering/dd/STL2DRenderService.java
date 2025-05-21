package it.aredegalli.printer.service.rendering.dd;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

public interface STL2DRenderService {
    BufferedImage render(Supplier<InputStream> supplier,
                         int width, int height) throws IOException;
}
