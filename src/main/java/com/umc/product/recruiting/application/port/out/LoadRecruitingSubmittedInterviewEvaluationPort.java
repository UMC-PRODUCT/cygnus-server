package com.umc.product.recruiting.application.port.out;

public interface LoadRecruitingSubmittedInterviewEvaluationPort {

    boolean existsSubmittedByRoundId(Long roundId);

    boolean existsSubmittedByApplicationId(Long applicationId);
}
