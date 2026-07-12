package com.umc.product.recruiting.application.service.command;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateDraftFormResponseCommand;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand.AnswerEntry;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

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
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final ManageFormResponseUseCase manageFormResponseUseCase;
    private final GetRecruitingApplicationQuestionScopeUseCase getQuestionScopeUseCase;
    private final RecruitingApplicationValidationService validationService;
    private final RecruitingApplicationKeyIssuer applicationKeyIssuer;
    private final RecruitingConcurrencyLockService concurrencyLockService;

    @Override
    public RecruitingApplicationCreatedInfo createDraft(CreateRecruitingApplicationDraftCommand command) {
        if (command.applicantMemberId() == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_MEMBER_REQUIRED);
        }
        RecruitingApplicationForm form = loadApplicationFormPort.getById(command.applicationFormId());
        validationService.validateApplicationPeriod(form, Instant.now());
        RecruitingApplicantProfile applicantProfile = createApplicantProfile(
            form,
            command.applicantName(),
            command.applicantEmail(),
            command.firstChoice(),
            command.secondChoice()
        );
        concurrencyLockService.lockNewApplicant(
            form.getRound(),
            command.applicantMemberId(),
            applicantProfile.getApplicantEmail()
        );
        validationService.validateNew(
            form.getRound(),
            command.applicantMemberId(),
            applicantProfile.getApplicantEmail()
        );
        String applicationKey = applicationKeyIssuer.issue(applicantProfile.getApplicantEmail());
        Long formResponseId = manageFormResponseUseCase.createDraft(CreateDraftFormResponseCommand.builder()
            .formId(form.getFormId())
            .respondentMemberId(command.applicantMemberId())
            .build());
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            formResponseId,
            command.applicantMemberId(),
            applicantProfile,
            applicationKey
        );
        RecruitingApplication saved = saveApplicationPort.save(application);
        return RecruitingApplicationCreatedInfo.of(saved.getId(), applicationKey, saved.getStatus());
    }

    @Override
    public RecruitingApplicationInfo updateDraft(UpdateRecruitingApplicationDraftCommand command) {
        String normalizedEmail = RecruitingApplicantEmail.from(command.applicantEmail()).value();
        RecruitingApplication application = loadDraftForApplicant(
            command.applicationId(),
            List.of(normalizedEmail)
        );
        application.validateApplicant(command.requesterMemberId());
        validationService.validateApplicationPeriod(application.getApplicationForm(), Instant.now());
        validationService.validateFormResponseOwnership(application, command.requesterMemberId());
        RecruitingApplicantProfile applicantProfile = createApplicantProfile(
            application.getApplicationForm(),
            command.applicantName(),
            command.applicantEmail(),
            command.firstChoice(),
            command.secondChoice()
        );
        validationService.validateUpdate(
            application.getRound(),
            application.getApplicantMemberId(),
            applicantProfile.getApplicantEmail(),
            application.getId()
        );
        application.updateDraft(command.requesterMemberId(), applicantProfile);
        manageFormResponseUseCase.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(application.getFormResponseId())
            .requesterMemberId(command.requesterMemberId())
            .answers(toAnswerCommands(command.answers()))
            .build());
        saveApplicationPort.save(application);
        return toInfo(application);
    }

    @Override
    public RecruitingApplicationInfo submit(SubmitRecruitingApplicationCommand command) {
        RecruitingApplication application = loadDraftForApplicant(command.applicationId(), List.of());
        application.validateApplicant(command.requesterMemberId());
        validationService.validateApplicationPeriod(application.getApplicationForm(), Instant.now());
        validationService.validateFormResponseOwnership(application, command.requesterMemberId());
        validationService.validateUpdate(
            application.getRound(),
            application.getApplicantMemberId(),
            application.getApplicantEmail(),
            application.getId()
        );
        RecruitingApplicationQuestionScopeInfo scope = getQuestionScopeUseCase.getQuestionScope(
            application.getApplicationForm().getId(),
            application.getFirstChoice(),
            application.getSecondChoice()
        );
        // TODO(#1146): Form 도메인이 전달받은 question ID의 Form 소속을 검증하는 공개 계약을 제공해야 한다.
        manageFormResponseUseCase.submitDraft(SubmitDraftFormResponseCommand.builder()
            .formResponseId(application.getFormResponseId())
            .requesterMemberId(command.requesterMemberId())
            .submittedIp(command.submittedIp())
            .requiredQuestionIds(scope.requiredQuestionIds())
            .allowedQuestionIds(scope.allowedQuestionIds())
            .build());
        application.submit(command.requesterMemberId());
        saveApplicationPort.save(application);
        return toInfo(application);
    }

    @Override
    public RecruitingApplicationInfo cancel(CancelRecruitingApplicationCommand command) {
        RecruitingApplication application = concurrencyLockService.lockApplicantThenApplication(
            command.applicationId(),
            List.of()
        );
        application.cancel(command.requesterMemberId(), command.reason());
        saveApplicationPort.save(application);
        return toInfo(application);
    }

    private RecruitingApplication loadDraftForApplicant(Long applicationId, List<String> additionalEmails) {
        RecruitingApplication application = concurrencyLockService.lockApplicantThenApplication(
            applicationId,
            additionalEmails
        );
        if (application.getStatus() != RecruitingApplicationStatus.DRAFT) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        return application;
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

    private RecruitingApplicantProfile createApplicantProfile(
        RecruitingApplicationForm form,
        String applicantName,
        String applicantEmail,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    ) {
        return RecruitingApplicantProfile.create(
            form.getRound(),
            applicantName,
            RecruitingApplicantEmail.from(applicantEmail),
            firstChoice,
            secondChoice
        );
    }

    private RecruitingApplicationInfo toInfo(RecruitingApplication application) {
        return RecruitingApplicationInfo.from(application);
    }

}
