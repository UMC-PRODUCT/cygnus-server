package com.umc.product.certificate.application.port.out.dto;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.certificate.domain.CertificateTemplate;

import lombok.Builder;

@Builder
public record CertificatePdfRenderCommand(
    String serialNumber,
    String issuanceNumber,
    CertificateTemplate template,
    String recipientName,
    String recipientSchoolName,
    Long gisuGeneration,
    String meritTitle,
    String meritDescription,
    Instant issuedAt,
    Instant expiresAt,
    String verificationUrl
) {

    public CertificatePdfRenderCommand {
        if (issuanceNumber == null) {
            issuanceNumber = serialNumber;
        }
        if (serialNumber == null) {
            serialNumber = issuanceNumber;
        }
        Objects.requireNonNull(issuanceNumber, "issuanceNumber must not be null");
        Objects.requireNonNull(recipientName, "recipientName must not be null");
        Objects.requireNonNull(gisuGeneration, "gisuGeneration must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(verificationUrl, "verificationUrl must not be null");
    }
}
