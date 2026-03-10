package com.gulab.sigkillserver.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProfileProperties {

    private final Security security = new Security();
    private final WebSocket webSocket = new WebSocket();
    private final OpenApi openApi = new OpenApi();

    public Security getSecurity() {
        return security;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public OpenApi getOpenApi() {
        return openApi;
    }

    public static class Security {
        private List<String> permitAllPaths = new ArrayList<>();
        private List<String> corsAllowedOriginPatterns = new ArrayList<>();

        public List<String> getPermitAllPaths() {
            return permitAllPaths;
        }

        public void setPermitAllPaths(List<String> permitAllPaths) {
            this.permitAllPaths = permitAllPaths;
        }

        public List<String> getCorsAllowedOriginPatterns() {
            return corsAllowedOriginPatterns;
        }

        public void setCorsAllowedOriginPatterns(List<String> corsAllowedOriginPatterns) {
            this.corsAllowedOriginPatterns = corsAllowedOriginPatterns;
        }
    }

    public static class WebSocket {
        private List<String> allowedOriginPatterns = new ArrayList<>();

        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }
    }

    public static class OpenApi {
        private List<ServerSpec> servers = new ArrayList<>();

        public List<ServerSpec> getServers() {
            return servers;
        }

        public void setServers(List<ServerSpec> servers) {
            this.servers = servers;
        }
    }

    public static class ServerSpec {
        private String url;
        private String description;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
