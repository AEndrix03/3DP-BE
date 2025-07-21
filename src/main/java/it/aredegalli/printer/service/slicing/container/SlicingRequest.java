package it.aredegalli.printer.service.slicing.container;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlicingRequest {
    private String jobId;
    private String stlData; // Base64 encoded STL
    private Map<String, Object> config;
    private String modelName;
}