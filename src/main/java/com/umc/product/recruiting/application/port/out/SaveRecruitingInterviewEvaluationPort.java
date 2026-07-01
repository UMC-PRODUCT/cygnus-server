package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;

public interface SaveRecruitingInterviewEvaluationPort {

    RecruitingInterviewEvaluation saveEvaluation(RecruitingInterviewEvaluation evaluation);
}
