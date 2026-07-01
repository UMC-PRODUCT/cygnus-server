package com.umc.product.recruiting.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand.AnswerEntry;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.out.IssueRecruitingApplicationNoPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.survey.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.survey.application.port.in.command.dto.AnswerCommand;
import com.umc.product.survey.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.survey.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.survey.application.port.in.command.dto.UpdateDraftFormResponseCommand;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingApplicationCommandService implements
    CreateRecruitingApplicationDraftUseCase,
    UpdateRecruitingApplicationDraftUseCase,
    SubmitRecruitingApplicationUseCase,
    CancelRecruitingApplicationUseCase {

    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final ManageFormResponseUseCase manageFormResponseUseCase;
    private final IssueRecruitingApplicationNoPort issueApplicationNoPort;

    @Override
    public RecruitingApplicationInfo createDraft(CreateRecruitingApplicationDraftCommand command) {
        RecruitingApplicationForm form = loadApplicationFormPort.getById(command.applicationFormId());
        validatePublished(form);
        validateNoBlockingApplication(form, command.applicantIdentityKey(), null);

        Long formResponseId = manageFormResponseUseCase.createDraft(CreateDraftFormResponseCommand.builder()
            .formId(form.getFormId())
            .respondentMemberId(command.applicantMemberId())
            .build());
        RecruitingApplication application = RecruitingApplication.createDraft(
            form,
            formResponseId,
            command.applicantMemberId(),
            command.applicantIdentityKey(),
            issueApplicationNoPort.issue(),
            command.maskedEmail()
        );
        return toInfo(saveApplicationPort.save(application));
    }

    @Override
    public RecruitingApplicationInfo updateDraft(UpdateRecruitingApplicationDraftCommand command) {
        RecruitingApplication application = loadDraft(command.applicationId());
        manageFormResponseUseCase.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(application.getFormResponseId())
            .requesterMemberId(command.requesterMemberId())
            .answers(toAnswerCommands(command.answers()))
            .build());
        return toInfo(application);
    }

    @Override
    public RecruitingApplicationInfo submit(SubmitRecruitingApplicationCommand command) {
        RecruitingApplication application = loadDraft(command.applicationId());
        validateNoBlockingApplication(application.getApplicationForm(), application.getApplicantIdentityKey(),
            application.getId());

        manageFormResponseUseCase.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(application.getFormResponseId())
            .requesterMemberId(command.requesterMemberId())
            .submittedIp(command.submittedIp())
            .build());
        application.submit(command.requesterMemberId());
        saveApplicationPort.save(application);
        return toInfo(application);
    }

    @Override
    public RecruitingApplicationInfo cancel(CancelRecruitingApplicationCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        application.cancel(command.requesterMemberId(), command.reason());
        saveApplicationPort.save(application);
        return toInfo(application);
    }

    private RecruitingApplication loadDraft(Long applicationId) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(applicationId);
        if (application.getStatus() != RecruitingApplicationStatus.DRAFT) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        return application;
    }

    private void validatePublished(RecruitingApplicationForm form) {
        if (form.getStatus() != RecruitingApplicationFormStatus.PUBLISHED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_PUBLISHED);
        }
    }

    private void validateNoBlockingApplication(
        RecruitingApplicationForm form,
        String applicantIdentityKey,
        Long excludedApplicationId
    ) {
        RecruitingRound round = form.getRound();
        RecruitingSeason season = round.getSeason();
        boolean hasSameRoundApplication = excludedApplicationId == null
            ? loadApplicationPort.existsByRoundIdAndApplicantIdentityKey(round.getId(), applicantIdentityKey)
            : loadApplicationPort.existsByRoundIdAndApplicantIdentityKeyAndIdNot(
                round.getId(),
                applicantIdentityKey,
                excludedApplicationId
            );
        if (hasSameRoundApplication) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_ALREADY_EXISTS);
        }

        boolean hasDifferentSchoolApplication = excludedApplicationId == null
            ? loadApplicationPort.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
                season.getGisuId(),
                season.getSchoolId(),
                applicantIdentityKey
            )
            : loadApplicationPort.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKeyAndIdNot(
                season.getGisuId(),
                season.getSchoolId(),
                applicantIdentityKey,
                excludedApplicationId
            );
        if (hasDifferentSchoolApplication) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_DIFFERENT_SCHOOL_EXISTS);
        }

        boolean hasBlockingApplication = excludedApplicationId == null
            ? loadApplicationPort.existsBlockingApplicationByGisuIdAndApplicantIdentityKey(
                season.getGisuId(),
                applicantIdentityKey
            )
            : loadApplicationPort.existsBlockingApplicationByGisuIdAndApplicantIdentityKeyAndIdNot(
                season.getGisuId(),
                applicantIdentityKey,
                excludedApplicationId
            );
        if (hasBlockingApplication) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_REAPPLICATION_BLOCKED);
        }
    }

    private List<AnswerCommand> toAnswerCommands(List<AnswerEntry> answers) {
        return answers.stream()
            .map(answer -> AnswerCommand.builder()
                .questionId(answer.questionId())
                .textValue(answer.textValue())
                .selectedOptionIds(answer.selectedOptionIds())
                .fileIds(answer.fileIds())
                .build())
            .toList();
    }

    private RecruitingApplicationInfo toInfo(RecruitingApplication application) {
        return RecruitingApplicationInfo.from(
            application.getId(),
            application.getApplicationNo(),
            application.getStatus()
        );
    }
}
