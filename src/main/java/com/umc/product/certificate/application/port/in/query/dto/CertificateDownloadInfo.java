package com.umc.product.certificate.application.port.in.query.dto;

import java.time.Instant;

public record CertificateDownloadInfo(
    Long certificateId,
    String serialNumber,
    String downloadUrl,
    Instant expiresAt
) {

    public static CertificateDownloadInfo of(
        Long certificateId,
        String serialNumber,
        String downloadUrl,
        Instant expiresAt
    ) {
        return new CertificateDownloadInfo(certificateId, serialNumber, downloadUrl, expiresAt);
    }
}
