package it.aredegalli.printer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableRetry
@Slf4j
public class SlicingConfig {

    @Value("${slicing.engines.external.timeout-seconds:300}")
    private int timeoutSeconds;

    @Value("${slicing.engines.external.connection-timeout-seconds:30}")
    private int connectionTimeoutSeconds;

    @Bean("slicingRestTemplate")
    public RestTemplate slicingRestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring slicing RestTemplate with timeout: {}s, connection timeout: {}s",
                timeoutSeconds, connectionTimeoutSeconds);

        return builder
                .connectTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Bean("healthCheckRestTemplate")
    public RestTemplate healthCheckRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(15))
                .build();
    }
}