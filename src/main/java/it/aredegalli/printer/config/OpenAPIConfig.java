package it.aredegalli.printer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("3D Printer API")
                        .version("1.0.0")
                        .description("API Documentation for 3D Printer BE")
                        .contact(new Contact()
                                .name("Andrea Redegalli")
                                .url("https://aredegalli.it")
                                .email("redegalli03@gmail.com")
                        )
                );
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**", "/authorization/**")
                .build();
    }
}
