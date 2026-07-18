package com.umc.product.registry.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.engine-ownership")
public record EngineOwnershipProperties(boolean enforcementEnabled) {
}
