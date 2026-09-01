package com.pecuni.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pecuni.google")
public record GoogleProperties(String clientId) {
}
