package com.umc.product.certificate.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.certificate.application.port.in.query.dto.CertificateVerificationInfo;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateTemplate;

public record CertificateVerificationResponse(
    boolean valid,
    String status,
    CertificateTemplate template,
    CertificateIssuer issuer,
    Long gisuGeneration,
    String recipientName,
    Instant issuedAt,
    Instant expiresAt
) {

    public static CertificateVerificationResponse from(CertificateVerificationInfo info) {
        return new CertificateVerificationResponse(
            info.valid(),
            info.status(),
            info.template(),
            info.issuer(),
            info.gisuGeneration(),
            info.recipientName(),
            info.issuedAt(),
            info.expiresAt()
        );
    }
}
