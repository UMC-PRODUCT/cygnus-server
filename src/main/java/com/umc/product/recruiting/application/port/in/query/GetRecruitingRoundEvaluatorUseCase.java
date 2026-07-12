package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public interface GetRecruitingRoundEvaluatorUseCase {

    List<RecruitingRoundEvaluatorInfo> listByRoundIdAndStage(Long roundId, RecruitingEvaluatorStage stage);

    List<RecruitingRoundEvaluatorInfo> listByRoundIdAndStage(
        Long roundId,
        RecruitingEvaluatorStage stage,
        Long requesterMemberId
    );

    boolean canEvaluate(Long roundId, Long memberId, RecruitingEvaluatorStage stage);
}
