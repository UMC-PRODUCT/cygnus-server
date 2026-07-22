package com.umc.product.recruiting.application.service.query;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResourceInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundResourceInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingResourceQueryService implements GetRecruitingResourceUseCase {

    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;
    private final GetFormResponseUseCase getFormResponseUseCase;
    private final Clock clock;

    @Override
    public RecruitingRoundResourceInfo getRound(Long roundId, Long requesterMemberId) {
        RecruitingRound round = loadRoundPort.getById(roundId);
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.findByRoundId(roundId).orElse(null);
        boolean publiclyVisible = round.getStatus() != RecruitingRoundStatus.DRAFT
            && applicationForm != null
            && applicationForm.getStatus() != RecruitingApplicationFormStatus.DRAFT;
        if (!publiclyVisible && !hasReadPermission(requesterMemberId, round.getSeason().getId())) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);
        }
        return RecruitingRoundResourceInfo.of(round, applicationForm, clock.instant());
    }

    @Override
    public RecruitingApplicationResourceInfo getApplication(Long applicationId, Long requesterMemberId) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(applicationId);
        boolean applicantAccess = requesterMemberId != null
            && Objects.equals(application.getApplicantMemberId(), requesterMemberId);
        boolean reviewAccess = hasReadPermission(requesterMemberId, application.getRound().getSeason().getId())
            || requesterMemberId != null
                && getRoundEvaluatorUseCase.canEvaluate(application.getRound().getId(), requesterMemberId);
        if (!applicantAccess && !reviewAccess) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
        return RecruitingApplicationResourceInfo.of(application, applicantAccess, reviewAccess);
    }

    @Override
    public RecruitingPublicApplicationInfo getApplicantView(Long applicationId, Long requesterMemberId) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(applicationId);
        application.validateApplicant(requesterMemberId);
        FormResponseWithAnswersInfo response = getFormResponseUseCase.getResponseWithAnswers(
            application.getFormResponseId()
        );
        Instant now = clock.instant();
        RecruitingRound round = application.getRound();
        RecruitingPublicResultStatus documentResult = resolveDocumentResult(application.getStatus(), round, now);
        RecruitingPublicResultStatus finalResult = resolveFinalResult(application.getStatus(), round, now);
        return RecruitingPublicApplicationInfo.builder()
            .applicationId(application.getId())
            .roundId(round.getId())
            .seasonId(round.getSeason().getId())
            .status(application.getStatus())
            .registrationStatus(application.getRegistrationStatus())
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
            .answers(response.answers().stream().map(RecruitingPublicApplicationInfo.Answer::from).toList())
            .build();
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
            case INTERVIEW_ASSIGNED, INTERVIEW_SKIPPED, FINAL_PASSED, FINAL_FAILED ->
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

    private boolean hasReadPermission(Long requesterMemberId, Long seasonId) {
        return requesterMemberId != null && checkPermissionUseCase.check(
            requesterMemberId,
            ResourcePermission.of(ResourceType.RECRUITMENT, seasonId, PermissionType.READ)
        );
    }
}
