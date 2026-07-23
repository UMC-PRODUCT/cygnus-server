package com.umc.product.term.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.terms.reconsent")
public record TermConsentEnforcementProperties(boolean enabled) {
}
