package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
public interface GetRecruitingRoundEvaluatorUseCase {

    List<RecruitingRoundEvaluatorInfo> listByRoundId(Long roundId);

    List<RecruitingRoundEvaluatorInfo> listByRoundId(
        Long roundId,
        Long requesterMemberId
    );

    boolean canEvaluate(Long roundId, Long memberId);
}
