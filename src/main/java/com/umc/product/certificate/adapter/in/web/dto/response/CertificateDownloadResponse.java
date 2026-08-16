package com.umc.product.certificate.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.certificate.application.port.in.query.dto.CertificateDownloadInfo;

public record CertificateDownloadResponse(
    Long certificateId,
    String serialNumber,
    String downloadUrl,
    Instant expiresAt
) {

    public static CertificateDownloadResponse from(CertificateDownloadInfo info) {
        return new CertificateDownloadResponse(
            info.certificateId(),
            info.serialNumber(),
            info.downloadUrl(),
            info.expiresAt()
        );
    }
}
