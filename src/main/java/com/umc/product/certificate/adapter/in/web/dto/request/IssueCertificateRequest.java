package com.umc.product.certificate.adapter.in.web.dto.request;

import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.domain.CertificateType;

import jakarta.validation.constraints.NotNull;

public record IssueCertificateRequest(
    @NotNull(message = "인증서 종류는 필수입니다.") CertificateType type,

    @NotNull(message = "기수 ID는 필수입니다.") Long gisuId,

    Long projectId
) {

    public IssueCertificateCommand toCommand(Long requesterMemberId) {
        return IssueCertificateCommand.builder()
            .type(type)
            .requesterMemberId(requesterMemberId)
            .gisuId(gisuId)
            .projectId(projectId)
            .build();
    }
}
