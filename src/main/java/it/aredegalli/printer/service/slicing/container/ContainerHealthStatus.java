package it.aredegalli.printer.service.slicing.container;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContainerHealthStatus {
    private final boolean healthy;
    private final String slicerVersion;
    private final long availableMemory;
    private final int activeJobs;
    private final String errorMessage;

    public static ContainerHealthStatus healthy(String version, long memory, int jobs) {
        return new ContainerHealthStatus(true, version, memory, jobs, null);
    }

    public static ContainerHealthStatus unhealthy(String error) {
        return new ContainerHealthStatus(false, null, 0, 0, error);
    }
}