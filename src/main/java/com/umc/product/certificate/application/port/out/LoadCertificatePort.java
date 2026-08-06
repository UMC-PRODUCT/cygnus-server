package com.umc.product.certificate.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.umc.product.certificate.domain.Certificate;
import com.umc.product.certificate.domain.CertificateTemplate;

public interface LoadCertificatePort {

    Optional<Certificate> findById(Long certificateId);

    Certificate getById(Long certificateId);

    Optional<Certificate> findBySerialNumber(String serialNumber);

    Optional<Certificate> findValidByScope(
        CertificateTemplate template,
        Long recipientMemberId,
        Long gisuId,
        String meritTitle,
        Instant now
    );

    boolean existsBySerialNumber(String serialNumber);

    List<Certificate> listByRecipientMemberId(Long memberId);
}
