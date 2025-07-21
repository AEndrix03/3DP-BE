package it.aredegalli.printer.service.slicing.container;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlicingResponse {
    private boolean success;
    private String jobId;
    private String gcodeData; // Base64 encoded G-code
    private SlicingMetrics metrics;
    private String error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlicingMetrics {
        private int lines;
        private int estimatedTime;
        private int layerCount;
    }
}