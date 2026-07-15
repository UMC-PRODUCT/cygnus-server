package com.umc.product.recruiting.application.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingInterviewQuestionQueryService implements GetRecruitingInterviewQuestionUseCase {

    private final LoadRecruitingRoundInterviewQuestionPort loadRoundQuestionPort;
    private final LoadRecruitingApplicationInterviewQuestionPort loadApplicationQuestionPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;

    @Override
    public List<RecruitingRoundInterviewQuestionInfo> listActiveRoundQuestions(
        Long roundId,
        Long requesterMemberId
    ) {
        RecruitingRound round = loadRoundPort.getById(roundId);
        authorizeQuestionRead(requesterMemberId, round.getId(), round.getSeason().getId());
        return loadRoundQuestionPort.listActiveByRoundId(roundId).stream()
            .map(RecruitingRoundInterviewQuestionInfo::from)
            .toList();
    }

    @Override
    public List<RecruitingApplicationInterviewQuestionInfo> listActiveApplicationQuestions(
        Long applicationId,
        Long requesterMemberId
    ) {
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        authorizeQuestionRead(
            requesterMemberId,
            application.getRound().getId(),
            application.getRound().getSeason().getId()
        );
        return loadApplicationQuestionPort.listActiveByApplicationId(applicationId).stream()
            .map(RecruitingApplicationInterviewQuestionInfo::from)
            .toList();
    }

    private void authorizeQuestionRead(Long requesterMemberId, Long roundId, Long seasonId) {
        if (authorizeManagementUseCase.canManageSeason(requesterMemberId, seasonId)) {
            return;
        }
        if (loadEvaluatorPort.existsByRoundIdAndMemberId(roundId, requesterMemberId)) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED);
    }
}
