package com.umc.product.certificate.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.umc.product.certificate.domain.exception.CertificateErrorCode;
import com.umc.product.certificate.domain.exception.CertificateException;
import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "certificate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certificate extends BaseEntity {

    private static final long VALID_DAYS = 365;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CertificateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CertificateIssuer issuer;

    @Column(nullable = false)
    private Long recipientMemberId;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(length = 100)
    private String recipientSchoolName;

    @Column(nullable = false)
    private Long gisuId;

    @Column(nullable = false)
    private Long gisuGeneration;

    private Long projectId;

    @Column(length = 100)
    private String projectName;

    @Column(length = 100)
    private String meritTitle;

    @Column(length = 500)
    private String meritDescription;

    @Column(nullable = false)
    private Long issuedByMemberId;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private Long revokedByMemberId;

    @Column(length = 500)
    private String revokeReason;

    @Column(nullable = false, length = 100)
    private String fileId;

    @Column(nullable = false, length = 64)
    private String fileSha256;

    public static Certificate issue(CertificateIssueSpec spec) {
        Certificate certificate = new Certificate();
        certificate.serialNumber = spec.serialNumber();
        certificate.type = spec.type();
        certificate.status = CertificateStatus.ISSUED;
        certificate.issuer = spec.issuer();
        certificate.recipientMemberId = spec.recipientMemberId();
        certificate.recipientName = spec.recipientName();
        certificate.recipientSchoolName = spec.recipientSchoolName();
        certificate.gisuId = spec.gisuId();
        certificate.gisuGeneration = spec.gisuGeneration();
        certificate.projectId = spec.projectId();
        certificate.projectName = spec.projectName();
        certificate.meritTitle = spec.meritTitle();
        certificate.meritDescription = spec.meritDescription();
        certificate.issuedByMemberId = spec.issuedByMemberId();
        certificate.issuedAt = spec.issuedAt();
        certificate.expiresAt = spec.issuedAt().plus(VALID_DAYS, ChronoUnit.DAYS);
        certificate.fileId = spec.fileId();
        certificate.fileSha256 = spec.fileSha256();
        return certificate;
    }

    public boolean isValidAt(Instant now) {
        return status == CertificateStatus.ISSUED && now.isBefore(expiresAt);
    }

    public CertificateStatus statusAt(Instant now) {
        if (status == CertificateStatus.REVOKED) {
            return CertificateStatus.REVOKED;
        }
        if (!now.isBefore(expiresAt)) {
            return CertificateStatus.EXPIRED;
        }
        return CertificateStatus.ISSUED;
    }

    public void revoke(Long revokedByMemberId, Instant revokedAt, String reason) {
        if (status == CertificateStatus.REVOKED) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ALREADY_REVOKED);
        }
        this.status = CertificateStatus.REVOKED;
        this.revokedByMemberId = revokedByMemberId;
        this.revokedAt = revokedAt;
        this.revokeReason = reason;
    }
}
