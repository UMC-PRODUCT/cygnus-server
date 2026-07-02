package com.umc.product.certificate.application.port.in.query.dto;

import java.util.Objects;

import com.umc.product.certificate.domain.CertificateTemplate;

import lombok.Builder;

@Builder
public record CertificatePdfPreviewQuery(
    CertificateTemplate template,
    String issuanceNumber,
    String recipientName,
    String recipientSchoolName,
    Long gisuGeneration,
    String projectName,
    String meritTitle,
    String meritDescription,
    String verificationUrl,
    String requestOrigin
) {

    public CertificatePdfPreviewQuery {
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(issuanceNumber, "issuanceNumber must not be null");
        Objects.requireNonNull(recipientName, "recipientName must not be null");
        Objects.requireNonNull(gisuGeneration, "gisuGeneration must not be null");
    }
}
