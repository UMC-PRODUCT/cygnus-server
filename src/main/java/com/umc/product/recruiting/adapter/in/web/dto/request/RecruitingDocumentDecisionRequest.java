package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "서류 합불 결정 요청")
public record RecruitingDocumentDecisionRequest(
    @Schema(description = "서류 합격 또는 불합격 결정", example = "PASS")
    @NotNull RecruitingDecisionStatus decision,
    @Schema(description = "결정 사유 또는 운영진 메모", example = "서류 평가 기준을 충족했습니다.", maxLength = 255)
    @Size(max = 255) String reason
) {

    public DecideRecruitingDocumentCommand toCommand(Long applicationId, Long decidedByMemberId) {
        return DecideRecruitingDocumentCommand.builder()
            .applicationId(applicationId)
            .decision(decision)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }
}
