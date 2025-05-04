package it.aredegalli.printer.config;

import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class ActuatorConfig {

    @Bean
    public StatusAggregator statusAggregator() {
        return new SimpleStatusAggregator();
    }

    @Bean
    public HealthContributorRegistry healthContributorRegistry() {
        return new DefaultHealthContributorRegistry(Collections.emptyMap());
    }
}
