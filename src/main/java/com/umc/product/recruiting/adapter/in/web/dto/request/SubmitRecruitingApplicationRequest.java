package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원서 제출 요청")
public record SubmitRecruitingApplicationRequest(
    @Schema(description = "클라이언트가 전달한 제출 IP. 비어 있으면 서버가 확인한 원격 주소를 사용합니다.", example = "127.0.0.1")
    String submittedIp
) {

    public SubmitRecruitingApplicationCommand toCommand(
        Long applicationId,
        Long resolvedRequesterMemberId,
        String fallbackSubmittedIp
    ) {
        return SubmitRecruitingApplicationCommand.builder()
            .applicationId(applicationId)
            .requesterMemberId(resolvedRequesterMemberId)
            .submittedIp(submittedIp == null ? fallbackSubmittedIp : submittedIp)
            .build();
    }
}
