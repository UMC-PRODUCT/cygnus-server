package com.umc.product.recruiting.application.service.query;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingPublicApplicationQueryService implements GetAnonymousRecruitingApplicationUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final GetFormResponseUseCase getFormResponseUseCase;
    private final Clock clock;

    @Override
    public RecruitingPublicApplicationInfo getByCredential(String applicantEmail, String applicationKey) {
        String normalizedEmail = RecruitingApplicantEmail.from(applicantEmail).value();
        validateApplicationKey(applicationKey);
        RecruitingApplication application = loadApplicationPort
            .findByApplicantEmailAndApplicationKey(normalizedEmail, applicationKey)
            .filter(RecruitingApplication::isAnonymous)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
        application.validateAnonymousApplicant(normalizedEmail);
        FormResponseWithAnswersInfo formResponse = getFormResponseUseCase.getResponseWithAnswersByAccessKey(
            application.getFormResponseAccessKey()
        );
        validateLinkedFormResponse(application, formResponse);

        Instant now = clock.instant();
        RecruitingRound round = application.getRound();
        RecruitingPublicResultStatus documentResult = resolveDocumentResult(application.getStatus(), round, now);
        RecruitingPublicResultStatus finalResult = resolveFinalResult(application.getStatus(), round, now);
        return RecruitingPublicApplicationInfo.builder()
            .applicationId(application.getId())
            .applicantName(application.getApplicantName())
            .applicantEmail(application.getApplicantEmail())
            .firstChoice(application.getFirstChoice())
            .secondChoice(application.getSecondChoice())
            .submitted(application.getSubmittedAt() != null)
            .cancelled(application.getStatus() == RecruitingApplicationStatus.CANCELLED)
            .editable(application.isEditable() && round.isLocalApplicationPeriodOpenAt(
                now,
                application.getApplicationForm().getStatus()
            ))
            .documentResult(documentResult)
            .finalResult(finalResult)
            .acceptedTrack(finalResult == RecruitingPublicResultStatus.APPROVED
                ? application.getAcceptedTrack()
                : null)
            .answers(formResponse.answers().stream().map(RecruitingPublicApplicationInfo.Answer::from).toList())
            .build();
    }

    private void validateLinkedFormResponse(
        RecruitingApplication application,
        FormResponseWithAnswersInfo formResponse
    ) {
        if (!Objects.equals(formResponse.id(), application.getFormResponseId())
            || !Objects.equals(formResponse.formId(), application.getApplicationForm().getFormId())
            || formResponse.respondentMemberId() != null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
    }

    private RecruitingPublicResultStatus resolveDocumentResult(
        RecruitingApplicationStatus status,
        RecruitingRound round,
        Instant now
    ) {
        if (now.isBefore(round.getDocumentResultPublishedAt())) {
            return RecruitingPublicResultStatus.PENDING;
        }
        if (status == RecruitingApplicationStatus.DOCUMENT_FAILED) {
            return RecruitingPublicResultStatus.REJECTED;
        }
        return switch (status) {
            case DOCUMENT_PASSED, INTERVIEW_ASSIGNED, INTERVIEW_SKIPPED, FINAL_PASSED, FINAL_FAILED ->
                RecruitingPublicResultStatus.APPROVED;
            default -> RecruitingPublicResultStatus.PENDING;
        };
    }

    private RecruitingPublicResultStatus resolveFinalResult(
        RecruitingApplicationStatus status,
        RecruitingRound round,
        Instant now
    ) {
        if (now.isBefore(round.getFinalResultPublishedAt())) {
            return RecruitingPublicResultStatus.PENDING;
        }
        return switch (status) {
            case FINAL_PASSED -> RecruitingPublicResultStatus.APPROVED;
            case FINAL_FAILED -> RecruitingPublicResultStatus.REJECTED;
            default -> RecruitingPublicResultStatus.PENDING;
        };
    }

    private void validateApplicationKey(String applicationKey) {
        if (applicationKey == null || !applicationKey.matches("[A-Z0-9]{6}")) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_KEY);
        }
    }
}
