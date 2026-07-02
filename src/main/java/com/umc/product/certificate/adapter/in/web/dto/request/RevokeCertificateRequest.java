package com.umc.product.certificate.adapter.in.web.dto.request;

import com.umc.product.certificate.application.port.in.command.dto.RevokeCertificateCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeCertificateRequest(
    @NotBlank(message = "폐기 사유는 필수입니다.") @Size(max = 500, message = "폐기 사유는 500자 이하로 입력해주세요.") String reason
) {

    public RevokeCertificateCommand toCommand(Long certificateId, Long requesterMemberId) {
        return RevokeCertificateCommand.builder()
            .certificateId(certificateId)
            .requesterMemberId(requesterMemberId)
            .reason(reason)
            .build();
    }
}
