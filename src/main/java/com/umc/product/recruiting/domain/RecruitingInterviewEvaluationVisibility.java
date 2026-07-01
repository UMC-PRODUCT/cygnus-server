package com.umc.product.recruiting.domain;

import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;

public final class RecruitingInterviewEvaluationVisibility {

    private RecruitingInterviewEvaluationVisibility() {
    }

    public static boolean canReadPeerEvaluations(RecruitingInterviewEvaluation ownEvaluation) {
        return ownEvaluation.getStatus() == RecruitingInterviewEvaluationStatus.SUBMITTED;
    }
}
