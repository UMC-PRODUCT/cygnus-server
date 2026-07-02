package com.umc.product.certificate.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.certificate.application.port.in.query.dto.CertificateInfo;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateStatus;
import com.umc.product.certificate.domain.CertificateType;

public record CertificateResponse(
    Long certificateId,
    String serialNumber,
    CertificateType type,
    CertificateIssuer issuer,
    CertificateStatus status,
    String recipientName,
    Long gisuId,
    Long gisuGeneration,
    Long projectId,
    String projectName,
    String meritTitle,
    Instant issuedAt,
    Instant expiresAt
) {

    public static CertificateResponse from(CertificateInfo info) {
        return new CertificateResponse(
            info.certificateId(),
            info.serialNumber(),
            info.type(),
            info.issuer(),
            info.status(),
            info.recipientName(),
            info.gisuId(),
            info.gisuGeneration(),
            info.projectId(),
            info.projectName(),
            info.meritTitle(),
            info.issuedAt(),
            info.expiresAt()
        );
    }
}
