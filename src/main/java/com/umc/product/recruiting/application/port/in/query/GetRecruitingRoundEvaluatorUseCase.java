package com.umc.product.recruiting.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundEvaluatorInfo;
public interface GetRecruitingRoundEvaluatorUseCase {

    List<RecruitingRoundEvaluatorInfo> listByRoundId(Long roundId);

    Map<Long, List<RecruitingRoundEvaluatorInfo>> listByRoundIds(Set<Long> roundIds);

    List<RecruitingRoundEvaluatorInfo> listByRoundId(
        Long roundId,
        Long requesterMemberId
    );

    boolean canEvaluate(Long roundId, Long memberId);
}
