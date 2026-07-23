package com.umc.product.recruiting.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationInterviewQuestionPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingApplicationInterviewQuestionCommandService
    implements ManageRecruitingApplicationInterviewQuestionUseCase {

    private final LoadRecruitingApplicationInterviewQuestionPort loadQuestionPort;
    private final SaveRecruitingApplicationInterviewQuestionPort saveQuestionPort;
    private final LoadRecruitingRoundEvaluatorPort loadEvaluatorPort;
    private final RecruitingInterviewQuestionMutationPolicy mutationPolicy;
    private final RecruitingConcurrencyLockService concurrencyLockService;

    @Override
    public Long createApplicationQuestion(CreateRecruitingApplicationInterviewQuestionCommand command) {
        RecruitingApplication application = concurrencyLockService.lockRoundThenApplication(command.applicationId());
        authorizeInterviewEvaluator(application, command.requesterMemberId());
        RecruitingApplicationInterviewQuestion question = RecruitingApplicationInterviewQuestion.create(
            application,
            command.content(),
            command.orderNo()
        );
        mutationPolicy.assertMutable(question);
        return saveQuestionPort.save(question).getId();
    }

    @Override
    public void updateApplicationQuestion(UpdateRecruitingApplicationInterviewQuestionCommand command) {
        RecruitingApplication application = concurrencyLockService.lockRoundThenApplication(command.applicationId());
        authorizeInterviewEvaluator(application, command.requesterMemberId());
        RecruitingApplicationInterviewQuestion question = loadQuestionPort.getById(command.questionId());
        validateScope(question, command.applicationId());
        mutationPolicy.assertMutable(question);
        question.updateBeforeFirstEvaluationSubmission(command.content(), command.orderNo());
        saveQuestionPort.save(question);
    }

    @Override
    public void deactivateApplicationQuestion(DeactivateRecruitingApplicationInterviewQuestionCommand command) {
        RecruitingApplication application = concurrencyLockService.lockRoundThenApplication(command.applicationId());
        authorizeInterviewEvaluator(application, command.requesterMemberId());
        RecruitingApplicationInterviewQuestion question = loadQuestionPort.getById(command.questionId());
        validateScope(question, command.applicationId());
        mutationPolicy.assertMutable(question);
        question.deactivateBeforeFirstEvaluationSubmission();
        saveQuestionPort.save(question);
    }

    private void authorizeInterviewEvaluator(RecruitingApplication application, Long requesterMemberId) {
        if (!loadEvaluatorPort.existsByRoundIdAndMemberId(
            application.getRound().getId(),
            requesterMemberId
        )) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED);
        }
    }

    private void validateScope(RecruitingApplicationInterviewQuestion question, Long applicationId) {
        if (!Objects.equals(question.getApplication().getId(), applicationId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED);
        }
    }
}
