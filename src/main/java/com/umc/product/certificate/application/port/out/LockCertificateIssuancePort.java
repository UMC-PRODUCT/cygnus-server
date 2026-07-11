package com.umc.product.certificate.application.port.out;

import com.umc.product.certificate.domain.CertificateTemplate;

public interface LockCertificateIssuancePort {

    void lockScope(
        CertificateTemplate template,
        Long recipientMemberId,
        Long gisuId,
        String meritTitle
    );
}
