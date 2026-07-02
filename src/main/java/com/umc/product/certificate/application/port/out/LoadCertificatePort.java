package com.umc.product.certificate.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateType;

public interface LoadCertificatePort {

    Optional<Certificate> findById(Long certificateId);

    Certificate getById(Long certificateId);

    Optional<Certificate> findBySerialNumber(String serialNumber);

    Optional<Certificate> findValidByScope(
        CertificateType type,
        CertificateIssuer issuer,
        Long recipientMemberId,
        Long gisuId,
        Long projectId,
        String meritTitle,
        Instant now
    );

    boolean existsBySerialNumber(String serialNumber);

    List<Certificate> listByRecipientMemberId(Long memberId);
}
