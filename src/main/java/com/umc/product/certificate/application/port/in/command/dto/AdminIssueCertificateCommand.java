package com.umc.product.certificate.application.port.in.command.dto;

import java.util.Objects;

import com.umc.product.certificate.domain.CertificateTemplate;

import lombok.Builder;

@Builder
public record AdminIssueCertificateCommand(
    CertificateTemplate template,
    Long requesterMemberId,
    Long recipientMemberId,
    Long gisuId,
    String meritTitle,
    String meritDescription,
    boolean reissue
) {

    public AdminIssueCertificateCommand {
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(recipientMemberId, "recipientMemberId must not be null");
        Objects.requireNonNull(gisuId, "gisuId must not be null");
        Objects.requireNonNull(template, "template must not be null");
    }
}
