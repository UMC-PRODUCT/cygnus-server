package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;

public interface SaveRecruitingRoundEvaluatorPort {

    RecruitingRoundEvaluator save(RecruitingRoundEvaluator evaluator);

    void delete(RecruitingRoundEvaluator evaluator);
}
