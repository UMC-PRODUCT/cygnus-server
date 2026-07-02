package com.umc.product.certificate.application.port.in.command.dto;

import java.util.Objects;

import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.CertificateType;

import lombok.Builder;

@Builder
public record AdminIssueCertificateCommand(
    CertificateTemplate template,
    CertificateType type,
    CertificateIssuer issuer,
    Long requesterMemberId,
    Long recipientMemberId,
    Long gisuId,
    Long projectId,
    String meritTitle,
    String meritDescription,
    boolean reissue
) {

    public AdminIssueCertificateCommand {
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(recipientMemberId, "recipientMemberId must not be null");
        Objects.requireNonNull(gisuId, "gisuId must not be null");
        Objects.requireNonNull(template, "template must not be null");
        if (type != null || issuer != null) {
            throw new IllegalArgumentException("type and issuer must be null when template is provided");
        }
    }
}
