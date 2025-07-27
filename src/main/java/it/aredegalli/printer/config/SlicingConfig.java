package it.aredegalli.printer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for slicing services including RestTemplate and async execution
 */
@Configuration
@EnableAsync
@EnableRetry
@Slf4j
public class SlicingConfig {

    @Value("${slicing.engines.external.timeout-seconds:300}")
    private int timeoutSeconds;

    @Value("${slicing.engines.external.connection-timeout-seconds:30}")
    private int connectionTimeoutSeconds;

    @Value("${slicing.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${slicing.async.max-pool-size:4}")
    private int maxPoolSize;

    @Value("${slicing.async.queue-capacity:10}")
    private int queueCapacity;

    /**
     * RestTemplate with optimized timeouts for external slicing services
     */
    @Bean("slicingRestTemplate")
    public RestTemplate slicingRestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring slicing RestTemplate with timeout: {}s, connection timeout: {}s",
                timeoutSeconds, connectionTimeoutSeconds);

        return builder
                .connectTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * Dedicated thread pool executor for slicing operations
     * Limits concurrent slicing jobs to prevent overloading external services
     */
    @Bean("slicingExecutor")
    public Executor slicingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(300); // 5 minutes
        executor.setThreadNamePrefix("SlicingThread-");

        // Reject policy: caller runs (blocks adding new tasks when queue is full)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Configured slicing executor: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    /**
     * Health check RestTemplate with shorter timeouts
     */
    @Bean("healthCheckRestTemplate")
    public RestTemplate healthCheckRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(15))
                .build();
    }
}