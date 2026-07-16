package com.umc.product.recruiting.application.service.command;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnonymousFormResponseResult;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormResponseCommand;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateAnonymousRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateAnonymousRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand.AnswerEntry;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.term.application.port.in.query.GetTermUseCase;
import com.umc.product.term.application.port.in.query.dto.TermInfo;
import com.umc.product.term.domain.enums.TermType;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingApplicationCommandService implements
    CreateRecruitingApplicationDraftUseCase,
    CreateAnonymousRecruitingApplicationDraftUseCase,
    UpdateRecruitingApplicationDraftUseCase,
    UpdateAnonymousRecruitingApplicationUseCase,
    SubmitRecruitingApplicationUseCase,
    SubmitAnonymousRecruitingApplicationUseCase,
    CancelRecruitingApplicationUseCase {

    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final ManageFormResponseUseCase manageFormResponseUseCase;
    private final GetRecruitingApplicationQuestionScopeUseCase getQuestionScopeUseCase;
    private final RecruitingApplicationValidationService validationService;
    private final RecruitingApplicationKeyIssuer applicationKeyIssuer;
    private final RecruitingConcurrencyLockService concurrencyLockService;
    private final GetTermUseCase getTermUseCase;
    private final Clock clock;

    @Override
    public RecruitingApplicationCreatedInfo createDraft(CreateRecruitingApplicationDraftCommand command) {
        if (command.applicantMemberId() == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_MEMBER_REQUIRED);
        }
        RecruitingApplicationForm form = loadApplicationFormPort.getById(command.applicationFormId());
        validationService.validateApplicationPeriod(form, clock.instant());
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
    public RecruitingApplicationCreatedInfo createAnonymousDraft(
        CreateAnonymousRecruitingApplicationDraftCommand command
    ) {
        RecruitingApplicationForm form = loadApplicationFormPort.getById(command.applicationFormId());
        validationService.validateApplicationPeriod(form, clock.instant());
        validatePrivacyConsent(command.privacyTermId(), command.privacyAgreed());
        RecruitingApplicantProfile applicantProfile = createApplicantProfile(
            form,
            command.applicantName(),
            command.applicantEmail(),
            command.firstChoice(),
            command.secondChoice()
        );
        concurrencyLockService.lockNewApplicant(form.getRound(), null, applicantProfile.getApplicantEmail());
        validationService.validateNew(form.getRound(), null, applicantProfile.getApplicantEmail());
        String applicationKey = applicationKeyIssuer.issue(applicantProfile.getApplicantEmail());
        AnonymousFormResponseResult formResponse = manageFormResponseUseCase.createAnonymousDraft(
            CreateAnonymousDraftFormResponseCommand.builder()
                .formId(form.getFormId())
                .build()
        );
        RecruitingApplication application = RecruitingApplication.createAnonymousDraft(
            form,
            formResponse.formResponseId(),
            formResponse.responseAccessKey(),
            applicantProfile,
            applicationKey,
            command.privacyTermId(),
            clock.instant()
        );
        RecruitingApplication saved = saveApplicationPort.save(application);
        return RecruitingApplicationCreatedInfo.of(saved.getId(), applicationKey, saved.getStatus());
    }

    @Override
    public RecruitingApplicationInfo updateDraft(UpdateRecruitingApplicationDraftCommand command) {
        String normalizedEmail = RecruitingApplicantEmail.from(command.applicantEmail()).value();
        RecruitingApplication application = loadEditableForApplicant(
            command.applicationId(),
            List.of(normalizedEmail)
        );
        application.validateApplicant(command.requesterMemberId());
        validationService.validateApplicationPeriod(application.getApplicationForm(), clock.instant());
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
        RecruitingApplicationQuestionScopeInfo scope = getQuestionScope(application, applicantProfile);
        validateAnswerScope(command.answers(), scope);
        application.updateDraft(command.requesterMemberId(), applicantProfile);
        updateMemberFormResponse(application, command, scope);
        saveApplicationPort.save(application);
        return toInfo(application);
    }

    @Override
    public RecruitingApplicationInfo updateAnonymous(UpdateAnonymousRecruitingApplicationCommand command) {
        String credentialEmail = RecruitingApplicantEmail.from(command.credentialEmail()).value();
        String updatedEmail = RecruitingApplicantEmail.from(command.applicantEmail()).value();
        RecruitingApplication found = getAnonymousByCredential(credentialEmail, command.applicationKey());
        RecruitingApplication application = concurrencyLockService.lockApplicantThenApplication(
            found.getId(),
            List.of(updatedEmail)
        );
        application.validateAnonymousApplicant(credentialEmail);
        validationService.validateApplicationPeriod(application.getApplicationForm(), clock.instant());
        validationService.validateAnonymousFormResponseOwnership(application);
        RecruitingApplicantProfile applicantProfile = createApplicantProfile(
            application.getApplicationForm(),
            command.applicantName(),
            command.applicantEmail(),
            command.firstChoice(),
            command.secondChoice()
        );
        validationService.validateUpdate(
            application.getRound(),
            null,
            applicantProfile.getApplicantEmail(),
            application.getId()
        );
        RecruitingApplicationQuestionScopeInfo scope = getQuestionScope(application, applicantProfile);
        validateAnswerScope(command.answers(), scope);
        boolean submitted = application.getStatus() == RecruitingApplicationStatus.SUBMITTED;
        application.updateAnonymous(credentialEmail, applicantProfile);
        List<AnswerCommand> answers = toAnswerCommands(command.answers());
        if (submitted) {
            manageFormResponseUseCase.updateAnonymousResponse(UpdateAnonymousFormResponseCommand.builder()
                .responseAccessKey(application.getFormResponseAccessKey())
                .answers(answers)
                .requiredQuestionIds(scope.requiredQuestionIds())
                .allowedQuestionIds(scope.allowedQuestionIds())
                .build());
        } else {
            manageFormResponseUseCase.updateAnonymousDraft(UpdateAnonymousDraftFormResponseCommand.builder()
                .responseAccessKey(application.getFormResponseAccessKey())
                .answers(answers)
                .build());
        }
        saveApplicationPort.save(application);
        return toInfo(application);
    }

    @Override
    public RecruitingApplicationInfo submit(SubmitRecruitingApplicationCommand command) {
        RecruitingApplication application = loadDraftForApplicant(command.applicationId(), List.of());
        application.validateApplicant(command.requesterMemberId());
        validationService.validateApplicationPeriod(application.getApplicationForm(), clock.instant());
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
    public RecruitingApplicationInfo submitAnonymous(SubmitAnonymousRecruitingApplicationCommand command) {
        String credentialEmail = RecruitingApplicantEmail.from(command.credentialEmail()).value();
        RecruitingApplication found = getAnonymousByCredential(credentialEmail, command.applicationKey());
        RecruitingApplication application = concurrencyLockService.lockApplicantThenApplication(found.getId(), List.of());
        if (application.getStatus() != RecruitingApplicationStatus.DRAFT) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        application.validateAnonymousApplicant(credentialEmail);
        validationService.validateApplicationPeriod(application.getApplicationForm(), clock.instant());
        validationService.validateAnonymousFormResponseOwnership(application);
        validationService.validateUpdate(
            application.getRound(),
            null,
            application.getApplicantEmail(),
            application.getId()
        );
        RecruitingApplicationQuestionScopeInfo scope = getQuestionScopeUseCase.getQuestionScope(
            application.getApplicationForm().getId(),
            application.getFirstChoice(),
            application.getSecondChoice()
        );
        manageFormResponseUseCase.submitAnonymousDraft(SubmitAnonymousDraftFormResponseCommand.builder()
            .responseAccessKey(application.getFormResponseAccessKey())
            .submittedIp(command.submittedIp())
            .requiredQuestionIds(scope.requiredQuestionIds())
            .allowedQuestionIds(scope.allowedQuestionIds())
            .build());
        application.submitAnonymous(credentialEmail);
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

    private RecruitingApplication loadEditableForApplicant(Long applicationId, List<String> additionalEmails) {
        RecruitingApplication application = concurrencyLockService.lockApplicantThenApplication(
            applicationId,
            additionalEmails
        );
        if (!application.isEditable()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        return application;
    }

    private RecruitingApplication getAnonymousByCredential(String normalizedEmail, String applicationKey) {
        if (applicationKey == null || !applicationKey.matches("[A-Z0-9]{6}")) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_KEY);
        }
        return loadApplicationPort.findByApplicantEmailAndApplicationKey(normalizedEmail, applicationKey)
            .filter(RecruitingApplication::isAnonymous)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
    }

    private RecruitingApplicationQuestionScopeInfo getQuestionScope(
        RecruitingApplication application,
        RecruitingApplicantProfile applicantProfile
    ) {
        return getQuestionScopeUseCase.getQuestionScope(
            application.getApplicationForm().getId(),
            applicantProfile.getFirstChoice(),
            applicantProfile.getSecondChoice()
        );
    }

    private void updateMemberFormResponse(
        RecruitingApplication application,
        UpdateRecruitingApplicationDraftCommand command,
        RecruitingApplicationQuestionScopeInfo scope
    ) {
        List<AnswerCommand> answers = toAnswerCommands(command.answers());
        if (application.getStatus() == RecruitingApplicationStatus.SUBMITTED) {
            manageFormResponseUseCase.updateResponse(UpdateFormResponseCommand.builder()
                .formId(application.getApplicationForm().getFormId())
                .respondentMemberId(command.requesterMemberId())
                .answers(answers)
                .requiredQuestionIds(scope.requiredQuestionIds())
                .allowedQuestionIds(scope.allowedQuestionIds())
                .build());
            return;
        }
        manageFormResponseUseCase.updateDraft(UpdateDraftFormResponseCommand.builder()
            .formResponseId(application.getFormResponseId())
            .requesterMemberId(command.requesterMemberId())
            .answers(answers)
            .build());
    }

    private void validatePrivacyConsent(Long privacyTermId, boolean privacyAgreed) {
        if (!privacyAgreed || privacyTermId == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_PRIVACY_CONSENT);
        }
        TermInfo activePrivacyTerm = getTermUseCase.getTermsByType(TermType.PRIVACY);
        if (!privacyTermId.equals(activePrivacyTerm.id())) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_PRIVACY_CONSENT);
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

    private void validateAnswerScope(
        List<AnswerEntry> answers,
        RecruitingApplicationQuestionScopeInfo scope
    ) {
        if (answers == null || answers.stream().anyMatch(answer -> !scope.allowedQuestionIds().contains(answer.questionId()))) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_ANSWER_OUT_OF_SCOPE);
        }
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
