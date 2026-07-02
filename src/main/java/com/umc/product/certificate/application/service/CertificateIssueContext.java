package com.umc.product.certificate.application.service;

import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.CertificateType;

record CertificateIssueContext(
    CertificateType type,
    CertificateTemplate template,
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
    Long issuedByMemberId
) {
}
