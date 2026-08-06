package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원서 철회 요청")
public record CancelRecruitingApplicationRequest(
    @Schema(description = "철회 사유", example = "개인 사정으로 지원을 철회합니다.")
    String reason
) {

    public CancelRecruitingApplicationCommand toCommand(Long applicationId, Long resolvedRequesterMemberId) {
        return CancelRecruitingApplicationCommand.builder()
            .applicationId(applicationId)
            .requesterMemberId(resolvedRequesterMemberId)
            .reason(reason)
            .build();
    }
}
