package com.umc.product.recruiting.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "recruiting_application",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_application_application_no",
        columnNames = "application_no"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_round_id", nullable = false)
    private RecruitingRound round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_form_id", nullable = false)
    private RecruitingApplicationForm applicationForm;

    @Column(nullable = false, name = "form_response_id")
    private Long formResponseId;

    @Column(name = "applicant_member_id")
    private Long applicantMemberId;

    @Column(nullable = false, name = "applicant_identity_key", length = 128)
    private String applicantIdentityKey;

    @Column(nullable = false, name = "application_no", length = 64)
    private String applicationNo;

    @Column(name = "masked_email", length = 255)
    private String maskedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "registration_status")
    private RecruitingApplicationRegistrationStatus registrationStatus;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "status_changed_member_id")
    private Long statusChangedMemberId;

    @Column(name = "status_change_reason")
    private String statusChangeReason;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingApplication(
        RecruitingApplicationForm applicationForm,
        Long formResponseId,
        Long applicantMemberId,
        String applicantIdentityKey,
        String applicationNo,
        String maskedEmail
    ) {
        this.round = applicationForm.getRound();
        this.applicationForm = applicationForm;
        this.formResponseId = formResponseId;
        this.applicantMemberId = applicantMemberId;
        this.applicantIdentityKey = applicantIdentityKey;
        this.applicationNo = applicationNo;
        this.maskedEmail = maskedEmail;
        this.status = RecruitingApplicationStatus.DRAFT;
        this.registrationStatus = RecruitingApplicationRegistrationStatus.NOT_READY;
    }

    public static RecruitingApplication createDraft(
        RecruitingApplicationForm applicationForm,
        Long formResponseId,
        Long applicantMemberId,
        String applicantIdentityKey,
        String applicationNo,
        String maskedEmail
    ) {
        return RecruitingApplication.builder()
            .applicationForm(applicationForm)
            .formResponseId(formResponseId)
            .applicantMemberId(applicantMemberId)
            .applicantIdentityKey(applicantIdentityKey)
            .applicationNo(applicationNo)
            .maskedEmail(maskedEmail)
            .build();
    }

    public void submit(Long memberId) {
        validateStatus(RecruitingApplicationStatus.DRAFT);
        this.status = RecruitingApplicationStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        recordStatusChange(memberId, null);
    }

    public void cancel(Long memberId, String reason) {
        if (this.status != RecruitingApplicationStatus.DRAFT && this.status != RecruitingApplicationStatus.SUBMITTED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        this.status = RecruitingApplicationStatus.CANCELLED;
        recordStatusChange(memberId, reason);
    }

    public void passDocument(Long memberId, String reason) {
        validateStatus(RecruitingApplicationStatus.SUBMITTED);
        this.status = RecruitingApplicationStatus.DOCUMENT_PASSED;
        recordStatusChange(memberId, reason);
    }

    public void failDocument(Long memberId, String reason) {
        validateStatus(RecruitingApplicationStatus.SUBMITTED);
        this.status = RecruitingApplicationStatus.DOCUMENT_FAILED;
        recordStatusChange(memberId, reason);
    }

    public void skipInterview(Long memberId, String reason) {
        validateStatus(RecruitingApplicationStatus.DOCUMENT_PASSED);
        this.status = RecruitingApplicationStatus.INTERVIEW_SKIPPED;
        recordStatusChange(memberId, reason);
    }

    public void passFinal(Long memberId, String reason) {
        validateFinalDecisionSource();
        this.status = RecruitingApplicationStatus.FINAL_PASSED;
        recordStatusChange(memberId, reason);
    }

    public void failFinal(Long memberId, String reason) {
        validateFinalDecisionSource();
        this.status = RecruitingApplicationStatus.FINAL_FAILED;
        recordStatusChange(memberId, reason);
    }

    public void markRegistrationReady(Long memberId) {
        validateStatus(RecruitingApplicationStatus.FINAL_PASSED);
        this.registrationStatus = RecruitingApplicationRegistrationStatus.READY;
        recordStatusChange(memberId, null);
    }

    public void register(Long memberId) {
        validateStatus(RecruitingApplicationStatus.FINAL_PASSED);
        if (this.registrationStatus != RecruitingApplicationRegistrationStatus.READY) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        this.registrationStatus = RecruitingApplicationRegistrationStatus.REGISTERED;
        recordStatusChange(memberId, null);
    }

    public boolean allowsReapplication() {
        return this.status == RecruitingApplicationStatus.DOCUMENT_FAILED
            || this.status == RecruitingApplicationStatus.FINAL_FAILED
            || this.status == RecruitingApplicationStatus.CANCELLED;
    }

    public boolean blocksReapplication() {
        return !allowsReapplication();
    }

    private void validateFinalDecisionSource() {
        if (this.status == RecruitingApplicationStatus.DOCUMENT_PASSED
            || this.status == RecruitingApplicationStatus.INTERVIEW_SKIPPED
            || this.status == RecruitingApplicationStatus.INTERVIEW_ASSIGNED) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
    }

    private void validateStatus(RecruitingApplicationStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
    }

    private void recordStatusChange(Long memberId, String reason) {
        this.statusChangedMemberId = memberId;
        this.statusChangeReason = reason;
        this.statusChangedAt = Instant.now();
    }
}
