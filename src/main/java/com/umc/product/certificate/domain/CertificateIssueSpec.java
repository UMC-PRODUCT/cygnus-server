package com.umc.product.certificate.domain;

import java.time.Instant;
import java.util.Objects;

import lombok.Builder;

@Builder
public record CertificateIssueSpec(
    String serialNumber,
    CertificateType type,
    CertificateIssuer issuer,
    Long recipientMemberId,
    String recipientName,
    String recipientSchoolName,
    Long gisuId,
    Long gisuGeneration,
    Long projectId,
    String projectName,
    String meritTitle,
    String meritDescription,
    Long issuedByMemberId,
    Instant issuedAt,
    String fileId,
    String fileSha256
) {

    public CertificateIssueSpec {
        Objects.requireNonNull(serialNumber, "serialNumber must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(issuer, "issuer must not be null");
        Objects.requireNonNull(recipientMemberId, "recipientMemberId must not be null");
        Objects.requireNonNull(recipientName, "recipientName must not be null");
        Objects.requireNonNull(gisuId, "gisuId must not be null");
        Objects.requireNonNull(gisuGeneration, "gisuGeneration must not be null");
        Objects.requireNonNull(issuedByMemberId, "issuedByMemberId must not be null");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(fileSha256, "fileSha256 must not be null");
    }
}
