package edu.uptc.subjects.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI subjectsServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Subjects Service API")
                .version("1.0.0")
                .description("Manages the academic offering of the subjects-service module: Subjects, Teachers and Courses, "
                        + "with pagination, sorting, filtering and uniform error handling."));
    }
}