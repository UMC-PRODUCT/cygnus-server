package com.umc.product.recruiting.application.service.command;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.dto.FormResponseInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingApplicationValidationService {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final GetFormResponseUseCase getFormResponseUseCase;

    public void validateApplicationPeriod(RecruitingApplicationForm applicationForm, Instant currentTime) {
        if (applicationForm.getStatus() != RecruitingApplicationFormStatus.PUBLISHED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_PUBLISHED);
        }
        if (!applicationForm.getRound().isLocalApplicationPeriodOpenAt(
            currentTime,
            applicationForm.getStatus()
        )) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_PERIOD_CLOSED);
        }
        // TODO: Form 공개 상태와 응답 기간 조회 계약이 제공되면 local 접수창과 함께 검증한다.
    }

    public void validateNew(
        RecruitingRound round,
        Long applicantMemberId,
        String applicantEmail
    ) {
        validate(round, applicantMemberId, applicantEmail, null);
    }

    public void validateFormResponseOwnership(RecruitingApplication application, Long requesterMemberId) {
        FormResponseInfo formResponse = getFormResponseUseCase.findById(application.getFormResponseId())
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
        boolean ownedResponse = Objects.equals(formResponse.respondentMemberId(), requesterMemberId);
        boolean linkedForm = Objects.equals(
            formResponse.formId(),
            application.getApplicationForm().getFormId()
        );
        if (!ownedResponse || !linkedForm) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_APPLICANT_MISMATCH);
        }
    }

    public void validateUpdate(
        RecruitingRound round,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedApplicationId
    ) {
        validate(round, applicantMemberId, applicantEmail, excludedApplicationId);
    }

    private void validate(
        RecruitingRound round,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedApplicationId
    ) {
        if (hasSameRoundApplication(round.getId(), applicantMemberId, applicantEmail, excludedApplicationId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_ALREADY_EXISTS);
        }
        RecruitingSeason season = round.getSeason();
        if (loadApplicationPort.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicant(
            season.getGisuId(),
            season.getSchoolId(),
            applicantMemberId,
            applicantEmail,
            excludedApplicationId
        )) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_DIFFERENT_SCHOOL_EXISTS);
        }
        if (loadApplicationPort.existsBlockingApplicationByGisuIdAndApplicant(
            season.getGisuId(),
            applicantMemberId,
            applicantEmail,
            excludedApplicationId
        )) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_REAPPLICATION_BLOCKED);
        }
    }

    private boolean hasSameRoundApplication(
        Long roundId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedApplicationId
    ) {
        if (excludedApplicationId == null) {
            return loadApplicationPort.existsByRoundIdAndApplicantMemberId(roundId, applicantMemberId)
                || loadApplicationPort.existsByRoundIdAndApplicantEmail(roundId, applicantEmail);
        }
        return loadApplicationPort.existsByRoundIdAndApplicantMemberIdAndIdNot(
            roundId,
            applicantMemberId,
            excludedApplicationId
        ) || loadApplicationPort.existsByRoundIdAndApplicantEmailAndIdNot(
            roundId,
            applicantEmail,
            excludedApplicationId
        );
    }
}
