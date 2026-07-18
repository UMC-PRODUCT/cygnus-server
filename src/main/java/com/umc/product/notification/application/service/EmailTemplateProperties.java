package com.umc.product.notification.application.service;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification.email.template")
public record EmailTemplateProperties(
    List<String> allowedActionOrigins
) {

    public static final String SERVICE_ORIGIN = "https://university.neordinary.com";

    public EmailTemplateProperties {
        allowedActionOrigins = allowedActionOrigins == null || allowedActionOrigins.isEmpty()
            ? List.of(SERVICE_ORIGIN)
            : allowedActionOrigins.stream()
                .map(origin -> origin == null ? null : origin.strip())
                .toList();
    }
}
