package it.aredegalli.printer.config.minio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.time.Duration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String host;

    @Value("${minio.port}")
    private int port;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(30))
                .socketTimeout(Duration.ofMinutes(10))
                .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                .maxConnections(100)
                .build();

        ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMinutes(15))
                .apiCallAttemptTimeout(Duration.ofMinutes(10))
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(host + ":" + port))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .region(Region.of(region))
                .httpClient(httpClient)
                .overrideConfiguration(clientConfig)
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .chunkedEncodingEnabled(true)
                                .build()
                )
                .build();
    }
}