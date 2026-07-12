package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지원서 합불 결정 요청")
public record RecruitingDecisionRequest(
    @Schema(description = "합격 또는 불합격 결정", example = "PASS")
    @NotNull RecruitingDecisionStatus decision,
    @Schema(description = "최종 합격 트랙. PASS 결정에만 필요합니다.", example = "PLAN")
    ChallengerTrack acceptedTrack,
    @Schema(description = "결정 사유 또는 운영진 메모", example = "서류 평가 기준을 충족했습니다.")
    String reason
) {

    @AssertTrue(message = "PASS 결정에는 acceptedTrack이 필요하고 FAIL 결정에는 지정할 수 없습니다.") public boolean isAcceptedTrackValid() {
        if (decision == null) {
            return true;
        }
        return decision == RecruitingDecisionStatus.PASS ? acceptedTrack != null : acceptedTrack == null;
    }

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
            .acceptedTrack(acceptedTrack)
            .decidedByMemberId(decidedByMemberId)
            .reason(reason)
            .build();
    }
}
