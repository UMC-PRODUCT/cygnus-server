package com.umc.product.certificate.application.service;

import com.umc.product.certificate.domain.CertificateTemplate;

record CertificateIssueContext(
    CertificateTemplate template,
    Long recipientMemberId,
    String recipientName,
    String recipientSchoolName,
    Long gisuId,
    Long gisuGeneration,
    String meritTitle,
    String meritDescription,
    Long issuedByMemberId
) {
}
