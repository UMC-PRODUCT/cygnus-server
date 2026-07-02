package com.umc.product.certificate.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateStatus;
import com.umc.product.certificate.domain.CertificateType;

public record CertificateIssueInfo(
    Long certificateId,
    String serialNumber,
    CertificateType type,
    CertificateIssuer issuer,
    CertificateStatus status,
    Instant issuedAt,
    Instant expiresAt
) {

    public static CertificateIssueInfo from(Certificate certificate) {
        return new CertificateIssueInfo(
            certificate.getId(),
            certificate.getSerialNumber(),
            certificate.getType(),
            certificate.getIssuer(),
            certificate.getStatus(),
            certificate.getIssuedAt(),
            certificate.getExpiresAt()
        );
    }
}
