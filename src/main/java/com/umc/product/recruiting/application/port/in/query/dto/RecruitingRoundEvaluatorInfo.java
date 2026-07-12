package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingRoundEvaluatorInfo(
    Long id,
    Long roundId,
    Long memberId,
    RecruitingEvaluatorStage stage
) {

    public static RecruitingRoundEvaluatorInfo from(RecruitingRoundEvaluator evaluator) {
        return new RecruitingRoundEvaluatorInfo(
            evaluator.getId(),
            evaluator.getRound().getId(),
            evaluator.getMemberId(),
            evaluator.getStage()
        );
    }
}
