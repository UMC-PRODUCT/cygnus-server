package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;

public interface SaveRecruitingApplicationEvaluationPort {

    RecruitingApplicationEvaluation saveEvaluation(RecruitingApplicationEvaluation evaluation);
}
