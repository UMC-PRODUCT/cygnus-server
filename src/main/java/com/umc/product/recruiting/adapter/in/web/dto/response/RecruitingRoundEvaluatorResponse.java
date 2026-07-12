package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모집 차수 단계별 평가자 응답")
public record RecruitingRoundEvaluatorResponse(
    @Schema(description = "평가자 등록 ID", example = "1") Long id,
    @Schema(description = "모집 차수 ID", example = "20") Long roundId,
    @Schema(description = "평가자 회원 ID", example = "99") Long memberId,
    @Schema(description = "평가 단계", example = "DOCUMENT") RecruitingEvaluatorStage stage
) {

    public static RecruitingRoundEvaluatorResponse from(RecruitingRoundEvaluatorInfo info) {
        return new RecruitingRoundEvaluatorResponse(info.id(), info.roundId(), info.memberId(), info.stage());
    }
}
