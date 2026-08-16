package com.umc.product.certificate.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateStatus;
import com.umc.product.certificate.domain.CertificateTemplate;

public record CertificateIssueInfo(
    Long certificateId,
    String serialNumber,
    CertificateTemplate template,
    CertificateIssuer issuer,
    CertificateStatus status,
    Instant issuedAt,
    Instant expiresAt
) {

    public static CertificateIssueInfo from(Certificate certificate) {
        return new CertificateIssueInfo(
            certificate.getId(),
            certificate.getSerialNumber(),
            certificate.getTemplate(),
            certificate.getTemplate().issuer(),
            certificate.getStatus(),
            certificate.getIssuedAt(),
            certificate.getExpiresAt()
        );
    }
}
