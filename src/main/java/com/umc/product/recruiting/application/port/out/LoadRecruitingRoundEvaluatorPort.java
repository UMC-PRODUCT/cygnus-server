package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
public interface LoadRecruitingRoundEvaluatorPort {

    RecruitingRoundEvaluator getByRoundIdAndMemberId(Long roundId, Long memberId);

    List<RecruitingRoundEvaluator> listByRoundId(Long roundId);

    boolean existsByRoundIdAndMemberId(Long roundId, Long memberId);
}
