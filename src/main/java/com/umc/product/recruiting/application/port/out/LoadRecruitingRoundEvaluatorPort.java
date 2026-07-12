package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public interface LoadRecruitingRoundEvaluatorPort {

    RecruitingRoundEvaluator getByRoundIdAndMemberIdAndStage(
        Long roundId,
        Long memberId,
        RecruitingEvaluatorStage stage
    );

    List<RecruitingRoundEvaluator> listByRoundIdAndStage(Long roundId, RecruitingEvaluatorStage stage);

    boolean existsByRoundIdAndMemberIdAndStage(Long roundId, Long memberId, RecruitingEvaluatorStage stage);
}
