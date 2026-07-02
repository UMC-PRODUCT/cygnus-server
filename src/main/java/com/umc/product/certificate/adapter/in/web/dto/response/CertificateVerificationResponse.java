package com.umc.product.certificate.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.certificate.application.port.in.query.dto.CertificateVerificationInfo;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateType;

public record CertificateVerificationResponse(
    boolean valid,
    String status,
    CertificateType type,
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
            info.type(),
            info.issuer(),
            info.gisuGeneration(),
            info.recipientName(),
            info.issuedAt(),
            info.expiresAt()
        );
    }
}
