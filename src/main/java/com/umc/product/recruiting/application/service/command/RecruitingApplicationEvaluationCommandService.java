package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.SaveRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingApplicationEvaluationCommandService implements
    SaveRecruitingApplicationEvaluationUseCase,
    SubmitRecruitingApplicationEvaluationUseCase {

    private final LoadRecruitingApplicationEvaluationPort loadEvaluationPort;
    private final SaveRecruitingApplicationEvaluationPort saveEvaluationPort;
    private final GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    private final RecruitingConcurrencyLockService concurrencyLockService;

    @Override
    public Long saveDraft(SaveRecruitingApplicationEvaluationCommand command) {
        RecruitingApplication application = getAuthorizedApplication(
            command.applicationId(),
            command.requesterMemberId(),
            command.stage()
        );
        RecruitingApplicationEvaluation evaluation = loadEvaluationPort
            .findByApplicationIdAndEvaluatorMemberIdAndStage(
                command.applicationId(),
                command.requesterMemberId(),
                command.stage()
            )
            .orElseGet(() -> RecruitingApplicationEvaluation.createDraft(
                application,
                command.requesterMemberId(),
                command.stage()
            ));
        evaluation.updateDraft(command.decision(), command.comment());
        return saveEvaluationPort.saveEvaluation(evaluation).getId();
    }

    @Override
    public void submit(SubmitRecruitingApplicationEvaluationCommand command) {
        RecruitingApplication application = getAuthorizedApplication(
            command.applicationId(),
            command.requesterMemberId(),
            command.stage()
        );
        RecruitingApplicationEvaluation evaluation = loadEvaluationPort
            .findByApplicationIdAndEvaluatorMemberIdAndStage(
                command.applicationId(),
                command.requesterMemberId(),
                command.stage()
            )
            .orElseGet(() -> RecruitingApplicationEvaluation.createDraft(
                application,
                command.requesterMemberId(),
                command.stage()
            ));
        evaluation.submit(command.decision(), command.comment());
        saveEvaluationPort.saveEvaluation(evaluation);
    }

    private RecruitingApplication getAuthorizedApplication(
        Long applicationId,
        Long requesterMemberId,
        RecruitingEvaluatorStage stage
    ) {
        RecruitingApplication application = concurrencyLockService.lockRoundThenApplication(applicationId);
        if (!getRoundEvaluatorUseCase.canEvaluate(application.getRound().getId(), requesterMemberId, stage)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_ACCESS_DENIED);
        }
        validateApplicationStage(application, stage);
        return application;
    }

    private void validateApplicationStage(RecruitingApplication application, RecruitingEvaluatorStage stage) {
        RecruitingApplicationStatus status = application.getStatus();
        boolean stageOpen = stage == RecruitingEvaluatorStage.DOCUMENT
            ? status == RecruitingApplicationStatus.SUBMITTED
            : status == RecruitingApplicationStatus.INTERVIEW_ASSIGNED;
        if (!stageOpen) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_EVALUATION_INVALID);
        }
    }
}
