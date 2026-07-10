package com.umc.product.certificate.adapter.in.web.dto.request;

import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.domain.CertificateTemplate;

import jakarta.validation.constraints.NotNull;

public record IssueCertificateRequest(
    @NotNull(message = "template은 필수입니다.") CertificateTemplate template,

    @NotNull(message = "기수 ID는 필수입니다.") Long gisuId
) {

    public IssueCertificateCommand toCommand(Long requesterMemberId) {
        return IssueCertificateCommand.builder()
            .template(template)
            .requesterMemberId(requesterMemberId)
            .gisuId(gisuId)
            .build();
    }
}
