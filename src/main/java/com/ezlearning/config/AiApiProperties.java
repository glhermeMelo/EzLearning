package com.ezlearning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api")
public record AiApiProperties(
    ApiInfo reasoning,
    ApiInfo media
) {
    public record ApiInfo(String url, String key) {}
}
