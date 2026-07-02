package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지원서 합불 결정 요청")
public record RecruitingDecisionRequest(
    @Schema(description = "합격 또는 불합격 결정", example = "PASS")
    @NotNull RecruitingDecisionStatus decision,
    @Schema(description = "결정 사유 또는 운영진 메모", example = "서류 평가 기준을 충족했습니다.")
    String reason
) {

    public DecideRecruitingDocumentCommand toDocumentCommand(Long applicationId, Long decidedByMemberId) {
        return DecideRecruitingDocumentCommand.builder()
            .applicationId(applicationId)
            .decision(decision)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }

    public DecideRecruitingFinalCommand toFinalCommand(Long applicationId, Long decidedByMemberId) {
        return DecideRecruitingFinalCommand.builder()
            .applicationId(applicationId)
            .decision(decision)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }
}
