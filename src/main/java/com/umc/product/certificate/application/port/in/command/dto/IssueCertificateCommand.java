package com.umc.product.certificate.application.port.in.command.dto;

import java.util.Objects;

import com.umc.product.certificate.domain.CertificateType;

import lombok.Builder;

@Builder
public record IssueCertificateCommand(
    CertificateType type,
    Long requesterMemberId,
    Long gisuId,
    Long projectId
) {

    public IssueCertificateCommand {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(gisuId, "gisuId must not be null");
    }
}
