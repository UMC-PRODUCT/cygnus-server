package com.umc.product.recruiting.application.service.query;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewAssignmentPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluationVisibility;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingInterviewQueryService implements GetRecruitingInterviewEvaluationUseCase {

    private final LoadRecruitingInterviewEvaluationPort loadEvaluationPort;
    private final LoadRecruitingInterviewAssignmentPort loadAssignmentPort;

    @Override
    public List<RecruitingInterviewEvaluationInfo> listVisibleEvaluations(Long applicationId, Long evaluatorMemberId) {
        return loadEvaluationPort.findByApplicationIdAndEvaluatorMemberId(applicationId, evaluatorMemberId)
            .map(ownEvaluation -> listVisibleEvaluations(applicationId, evaluatorMemberId, ownEvaluation))
            .orElse(List.of());
    }

    @Override
    public boolean isAssignmentBelongsToSeason(Long assignmentId, Long seasonId) {
        if (assignmentId == null || seasonId == null) {
            return false;
        }
        RecruitingInterviewAssignment assignment = loadAssignmentPort.getAssignmentById(assignmentId);
        return Objects.equals(assignment.getApplication().getRound().getSeason().getId(), seasonId);
    }

    private List<RecruitingInterviewEvaluationInfo> listVisibleEvaluations(
        Long applicationId,
        Long evaluatorMemberId,
        RecruitingInterviewEvaluation ownEvaluation
    ) {
        List<RecruitingInterviewEvaluation> evaluations = loadEvaluationPort.listByApplicationId(applicationId);
        if (RecruitingInterviewEvaluationVisibility.canReadPeerEvaluations(ownEvaluation)) {
            return evaluations.stream()
                .map(RecruitingInterviewEvaluationInfo::from)
                .toList();
        }
        return evaluations.stream()
            .filter(evaluation -> evaluation.getEvaluatorMemberId().equals(evaluatorMemberId))
            .map(RecruitingInterviewEvaluationInfo::from)
            .toList();
    }
}
