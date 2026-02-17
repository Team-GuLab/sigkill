package com.gulab.sigkillserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().servers(List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Local"),
                new Server()
                        .url("https://sigkill-quiz.kr")
                        .description("Production")
        ));
    }
}
