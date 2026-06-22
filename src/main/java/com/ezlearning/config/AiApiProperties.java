package com.ezlearning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api.reasoning")
public record AiApiProperties(
    String url,
    String key
) {}
