package com.umc.product.certificate.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.certificate.application.port.in.command.dto.CertificateIssueInfo;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateStatus;
import com.umc.product.certificate.domain.CertificateType;

public record CertificateIssueResponse(
    Long certificateId,
    String serialNumber,
    CertificateType type,
    CertificateIssuer issuer,
    CertificateStatus status,
    Instant issuedAt,
    Instant expiresAt
) {

    public static CertificateIssueResponse from(CertificateIssueInfo info) {
        return new CertificateIssueResponse(
            info.certificateId(),
            info.serialNumber(),
            info.type(),
            info.issuer(),
            info.status(),
            info.issuedAt(),
            info.expiresAt()
        );
    }
}
