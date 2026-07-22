package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
public interface LoadRecruitingRoundEvaluatorPort {

    RecruitingRoundEvaluator getByRoundIdAndMemberId(Long roundId, Long memberId);

    List<RecruitingRoundEvaluator> listByRoundId(Long roundId);

    Map<Long, List<RecruitingRoundEvaluator>> listByRoundIds(Set<Long> roundIds);

    boolean existsByRoundIdAndMemberId(Long roundId, Long memberId);
}
