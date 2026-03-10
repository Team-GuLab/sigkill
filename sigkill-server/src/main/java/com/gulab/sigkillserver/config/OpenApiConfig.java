package com.gulab.sigkillserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod")
@RequiredArgsConstructor
public class OpenApiConfig {

    private final AppProfileProperties appProfileProperties;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = appProfileProperties.getOpenApi().getServers().stream()
                .map(serverSpec -> new Server()
                        .url(serverSpec.getUrl())
                        .description(serverSpec.getDescription()))
                .toList();

        return new OpenAPI().servers(servers);
    }
}
