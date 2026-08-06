package com.umc.product.certificate.application.port.in.query.dto;

import java.util.Objects;

public record CertificatePdfPreviewInfo(
    String fileName,
    byte[] content
) {

    public CertificatePdfPreviewInfo {
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }

    public static CertificatePdfPreviewInfo of(String fileName, byte[] content) {
        return new CertificatePdfPreviewInfo(fileName, content);
    }
}
