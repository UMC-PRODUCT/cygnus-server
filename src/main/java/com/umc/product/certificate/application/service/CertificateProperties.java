package com.umc.product.certificate.application.service;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

@ConfigurationProperties(prefix = "certificate")
public record CertificateProperties(String verificationUrlTemplate) {

    private static final String DEFAULT_VERIFICATION_URL_TEMPLATE = "/api/v1/certificates/verify/{serialNumber}";

    public CertificateProperties {
        if (!StringUtils.hasText(verificationUrlTemplate)) {
            verificationUrlTemplate = DEFAULT_VERIFICATION_URL_TEMPLATE;
        }
    }

    public String verificationUrl(String serialNumber) {
        String encodedSerialNumber = UriUtils.encodePathSegment(serialNumber, StandardCharsets.UTF_8);
        return verificationUrlTemplate.replace("{serialNumber}", encodedSerialNumber);
    }
}
