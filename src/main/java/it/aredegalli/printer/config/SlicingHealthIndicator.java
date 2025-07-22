package it.aredegalli.printer.config;

import it.aredegalli.printer.service.slicing.container.ContainerManager;
import it.aredegalli.printer.service.slicing.engine.SlicingEngineSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("slicingHealth")
@RequiredArgsConstructor
public class SlicingHealthIndicator implements HealthIndicator {

    private final SlicingEngineSelector engineSelector;
    private final ContainerManager containerManager;

    @Override
    public Health health() {
        try {
            Health.Builder builder = Health.up();

            // Check available engines
            Map<String, String> engines = engineSelector.getAvailableEngines();
            builder.withDetail("available_engines", engines.size());
            builder.withDetail("engines", engines);

            // Check default engine
            try {
                String defaultEngine = engineSelector.getDefaultEngine().getName();
                builder.withDetail("default_engine", defaultEngine);
            } catch (Exception e) {
                builder.down().withDetail("default_engine_error", e.getMessage());
                return builder.build();
            }

            // Check container status if available
            try {
                boolean hasContainers = containerManager.hasAvailableContainers();
                int healthyContainers = containerManager.getHealthyContainerCount();

                builder.withDetail("docker_containers_available", hasContainers);
                builder.withDetail("healthy_containers", healthyContainers);
                builder.withDetail("containers_by_type", containerManager.getContainersByType());

            } catch (Exception e) {
                // Container manager not available, but that's OK
                builder.withDetail("docker_status", "not_available");
            }

            // Overall status
            if (engines.isEmpty()) {
                return builder.down()
                        .withDetail("error", "No slicing engines available")
                        .build();
            }

            return builder.build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("type", e.getClass().getSimpleName())
                    .build();
        }
    }
}