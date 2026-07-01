package com.umc.product.recruiting.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluationVisibility;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingInterviewQueryService implements GetRecruitingInterviewEvaluationUseCase {

    private final LoadRecruitingInterviewEvaluationPort loadEvaluationPort;

    @Override
    public List<RecruitingInterviewEvaluationInfo> listVisibleEvaluations(Long applicationId, Long evaluatorMemberId) {
        return loadEvaluationPort.findByApplicationIdAndEvaluatorMemberId(applicationId, evaluatorMemberId)
            .map(ownEvaluation -> listVisibleEvaluations(applicationId, evaluatorMemberId, ownEvaluation))
            .orElse(List.of());
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
