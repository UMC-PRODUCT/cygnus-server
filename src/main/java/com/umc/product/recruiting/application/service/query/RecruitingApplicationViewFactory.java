package com.umc.product.recruiting.application.service.query;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.form.application.port.in.query.dto.FormResponseWithAnswersInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

/**
 * 지원자에게 공개하는 지원서 조회 정보를 생성한다.
 * <p>
 * 회원/비회원 조회 경로가 동일한 결과 공개 시각과 응답 표현을 사용하도록 공개 정책을 한곳에서 관리한다.
 */
final class RecruitingApplicationViewFactory {

    private RecruitingApplicationViewFactory() {
    }

    static RecruitingPublicApplicationInfo create(
        RecruitingApplication application,
        FormResponseWithAnswersInfo formResponse,
        Instant now
    ) {
        validateLinkedFormResponse(application, formResponse);

        RecruitingRound round = application.getRound();
        RecruitingPublicResultStatus documentResult = resolveDocumentResult(application.getStatus(), round, now);
        RecruitingPublicResultStatus finalResult = resolveFinalResult(application.getStatus(), round, now);
        return RecruitingPublicApplicationInfo.builder()
            .applicationId(application.getId())
            .gisuId(round.getSeason().getGisuId())
            .roundId(round.getId())
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

    private static void validateLinkedFormResponse(
        RecruitingApplication application,
        FormResponseWithAnswersInfo formResponse
    ) {
        if (!Objects.equals(formResponse.id(), application.getFormResponseId())
            || !Objects.equals(formResponse.formId(), application.getApplicationForm().getFormId())
            || !Objects.equals(formResponse.respondentMemberId(), application.getApplicantMemberId())) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
    }

    private static RecruitingPublicResultStatus resolveDocumentResult(
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

    private static RecruitingPublicResultStatus resolveFinalResult(
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
}
