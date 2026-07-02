package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;

public interface GetRecruitingInterviewEvaluationUseCase {

    default List<RecruitingInterviewEvaluationInfo> listVisibleEvaluations(Long applicationId, Long evaluatorMemberId) {
        return listVisibleEvaluations(applicationId, evaluatorMemberId, false);
    }

    List<RecruitingInterviewEvaluationInfo> listVisibleEvaluations(
        Long applicationId,
        Long evaluatorMemberId,
        boolean canBypassVisibility
    );

    boolean isAssignmentBelongsToSeason(Long assignmentId, Long seasonId);
}
