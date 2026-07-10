package com.umc.product.certificate.application.port.in.command.dto;

import java.util.Objects;

import com.umc.product.certificate.domain.CertificateTemplate;

import lombok.Builder;

@Builder
public record IssueCertificateCommand(
    CertificateTemplate template,
    Long requesterMemberId,
    Long gisuId
) {

    public IssueCertificateCommand {
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(gisuId, "gisuId must not be null");
    }
}
