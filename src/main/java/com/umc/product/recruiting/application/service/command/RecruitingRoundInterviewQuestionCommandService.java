package com.umc.product.recruiting.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingRoundInterviewQuestionCommandService implements ManageRecruitingRoundInterviewQuestionUseCase {

    private final LoadRecruitingRoundInterviewQuestionPort loadQuestionPort;
    private final SaveRecruitingRoundInterviewQuestionPort saveQuestionPort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    private final RecruitingInterviewQuestionMutationPolicy mutationPolicy;
    private final RecruitingConcurrencyLockService concurrencyLockService;

    @Override
    public Long createRoundQuestion(CreateRecruitingRoundInterviewQuestionCommand command) {
        RecruitingRound round = concurrencyLockService.lockRound(command.roundId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            round.getSeason().getId()
        );
        RecruitingRoundInterviewQuestion question = RecruitingRoundInterviewQuestion.create(
            round,
            command.content(),
            command.orderNo()
        );
        mutationPolicy.assertMutable(question);
        return saveQuestionPort.save(question).getId();
    }

    @Override
    public void updateRoundQuestion(UpdateRecruitingRoundInterviewQuestionCommand command) {
        RecruitingRound round = concurrencyLockService.lockRound(command.roundId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            round.getSeason().getId()
        );
        RecruitingRoundInterviewQuestion question = loadQuestionPort.getById(command.questionId());
        validateScope(question, command.roundId());
        mutationPolicy.assertMutable(question);
        question.updateBeforeFirstEvaluationSubmission(command.content(), command.orderNo());
        saveQuestionPort.save(question);
    }

    @Override
    public void deactivateRoundQuestion(DeactivateRecruitingRoundInterviewQuestionCommand command) {
        RecruitingRound round = concurrencyLockService.lockRound(command.roundId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            round.getSeason().getId()
        );
        RecruitingRoundInterviewQuestion question = loadQuestionPort.getById(command.questionId());
        validateScope(question, command.roundId());
        mutationPolicy.assertMutable(question);
        question.deactivateBeforeFirstEvaluationSubmission();
        saveQuestionPort.save(question);
    }

    private void validateScope(RecruitingRoundInterviewQuestion question, Long roundId) {
        if (!Objects.equals(question.getRound().getId(), roundId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED);
        }
    }
}
