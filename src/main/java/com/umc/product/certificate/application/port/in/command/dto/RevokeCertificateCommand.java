package com.umc.product.certificate.application.port.in.command.dto;

import java.util.Objects;

import lombok.Builder;

@Builder
public record RevokeCertificateCommand(
    Long certificateId,
    Long requesterMemberId,
    String reason
) {

    public RevokeCertificateCommand {
        Objects.requireNonNull(certificateId, "certificateId must not be null");
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be null or blank");
        }
    }
}
