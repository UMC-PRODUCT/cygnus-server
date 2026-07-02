package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;

public interface GetRecruitingInterviewEvaluationUseCase {

    List<RecruitingInterviewEvaluationInfo> listVisibleEvaluations(Long applicationId, Long evaluatorMemberId);

    boolean isAssignmentBelongsToSeason(Long assignmentId, Long seasonId);
}
