package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
public record RecruitingRoundEvaluatorInfo(
    Long id,
    Long roundId,
    Long memberId
) {

    public static RecruitingRoundEvaluatorInfo from(RecruitingRoundEvaluator evaluator) {
        return new RecruitingRoundEvaluatorInfo(
            evaluator.getId(),
            evaluator.getRound().getId(),
            evaluator.getMemberId()
        );
    }
}
