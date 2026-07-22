package com.umc.product.recruiting.application.service.command;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingInterviewQuestionsCommand;
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

    @Override
    public void replaceApplicationQuestions(ReplaceRecruitingInterviewQuestionsCommand.Application command) {
        RecruitingApplication application = concurrencyLockService.lockRoundThenApplication(command.applicationId());
        authorizeInterviewEvaluator(application, command.requesterMemberId());
        Map<Long, RecruitingApplicationInterviewQuestion> currentById = loadQuestionPort
            .listByApplicationId(command.applicationId())
            .stream()
            .collect(Collectors.toMap(RecruitingApplicationInterviewQuestion::getId, Function.identity()));
        Set<Long> retainedIds = new HashSet<>();
        for (ReplaceRecruitingInterviewQuestionsCommand.Entry entry : command.questions()) {
            if (entry.id() == null) {
                RecruitingApplicationInterviewQuestion created = RecruitingApplicationInterviewQuestion.create(
                    application,
                    entry.content(),
                    entry.orderNo()
                );
                mutationPolicy.assertMutable(created);
                saveQuestionPort.save(created);
                continue;
            }
            if (!retainedIds.add(entry.id())) {
                throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_INVALID_TARGET);
            }
            RecruitingApplicationInterviewQuestion current = currentById.get(entry.id());
            if (current == null) {
                throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED);
            }
            mutationPolicy.assertMutable(current);
            current.replaceBeforeFirstEvaluationSubmission(entry.content(), entry.orderNo());
            saveQuestionPort.save(current);
        }
        currentById.values().stream()
            .filter(RecruitingApplicationInterviewQuestion::isActive)
            .filter(question -> !retainedIds.contains(question.getId()))
            .forEach(question -> {
                mutationPolicy.assertMutable(question);
                question.deactivateBeforeFirstEvaluationSubmission();
                saveQuestionPort.save(question);
            });
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
