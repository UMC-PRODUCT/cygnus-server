package com.umc.product.recruiting.domain;

import java.time.Instant;
import java.util.regex.Pattern;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_recruiting_application_round_email",
            columnNames = {"recruiting_round_id", "applicant_email"}
        ),
        @UniqueConstraint(
            name = "uk_recruiting_application_round_member",
            columnNames = {"recruiting_round_id", "applicant_member_id"}
        ),
        @UniqueConstraint(
            name = "uk_recruiting_application_email_key",
            columnNames = {"applicant_email", "application_key"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingApplication extends BaseEntity {

    private static final Pattern APPLICATION_KEY_PATTERN = Pattern.compile("[A-Z0-9]{6}");

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

    @Embedded
    private RecruitingApplicantProfile applicantProfile;

    @Column(name = "privacy_term_id")
    private Long privacyTermId;

    @Column(name = "privacy_agreed_at")
    private Instant privacyAgreedAt;

    @Column(name = "application_key", nullable = false, length = 6)
    private String applicationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "accepted_track")
    private ChallengerTrack acceptedTrack;

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
        RecruitingApplicantProfile applicantProfile,
        String applicationKey
    ) {
        validateRequired(applicationForm, formResponseId, applicantMemberId, applicantProfile, applicationKey);
        this.round = applicationForm.getRound();
        this.applicationForm = applicationForm;
        this.formResponseId = formResponseId;
        this.applicantMemberId = applicantMemberId;
        this.applicantProfile = applicantProfile;
        this.applicationKey = applicationKey;
        this.status = RecruitingApplicationStatus.DRAFT;
        this.registrationStatus = RecruitingApplicationRegistrationStatus.NOT_READY;
    }

    public static RecruitingApplication createMemberDraft(
        RecruitingApplicationForm applicationForm,
        Long formResponseId,
        Long applicantMemberId,
        RecruitingApplicantProfile applicantProfile,
        String applicationKey
    ) {
        return RecruitingApplication.builder()
            .applicationForm(applicationForm)
            .formResponseId(formResponseId)
            .applicantMemberId(applicantMemberId)
            .applicantProfile(applicantProfile)
            .applicationKey(applicationKey)
            .build();
    }

    public void updateDraft(Long requesterMemberId, RecruitingApplicantProfile applicantProfile) {
        if (status != RecruitingApplicationStatus.DRAFT) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        validateApplicant(requesterMemberId);
        if (applicantProfile == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_REQUIRED_FIELD);
        }
        this.applicantProfile = applicantProfile;
    }

    public void recordPrivacyConsent(Long termId, Instant agreedAt) {
        if (termId == null || termId <= 0 || agreedAt == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_PRIVACY_CONSENT);
        }
        this.privacyTermId = termId;
        this.privacyAgreedAt = agreedAt;
    }

    public void acceptTrack(ChallengerTrack track) {
        if (!applicantProfile.includesTrack(track)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_ACCEPTED_TRACK);
        }
        this.acceptedTrack = track;
    }

    public void validateApplicant(Long memberId) {
        if (memberId == null || !memberId.equals(applicantMemberId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_APPLICANT_MISMATCH);
        }
    }

    public void submit(Long memberId) {
        validateApplicant(memberId);
        transitionTo(RecruitingApplicationStatus.SUBMITTED, memberId, null);
        this.submittedAt = Instant.now();
    }

    public void cancel(Long memberId, String reason) {
        validateApplicant(memberId);
        transitionTo(RecruitingApplicationStatus.CANCELLED, memberId, reason);
    }

    public void passDocument(Long memberId, String reason) {
        transitionTo(RecruitingApplicationStatus.DOCUMENT_PASSED, memberId, reason);
    }

    public void failDocument(Long memberId, String reason) {
        transitionTo(RecruitingApplicationStatus.DOCUMENT_FAILED, memberId, reason);
    }

    public void skipInterview(Long memberId, String reason) {
        transitionTo(RecruitingApplicationStatus.INTERVIEW_SKIPPED, memberId, reason);
    }

    public void assignInterview(Long memberId, String reason) {
        transitionTo(RecruitingApplicationStatus.INTERVIEW_ASSIGNED, memberId, reason);
    }

    public void passFinal(Long memberId, String reason, ChallengerTrack acceptedTrack) {
        acceptTrack(acceptedTrack);
        transitionTo(RecruitingApplicationStatus.FINAL_PASSED, memberId, reason);
    }

    public void failFinal(Long memberId, String reason) {
        transitionTo(RecruitingApplicationStatus.FINAL_FAILED, memberId, reason);
    }

    public void markRegistrationReady(Long memberId) {
        RecruitingApplicationLifecyclePolicy.requireFinalPassed(status);
        requireRegistrationStatus(RecruitingApplicationRegistrationStatus.NOT_READY);
        if (acceptedTrack == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_ACCEPTED_TRACK);
        }
        this.registrationStatus = RecruitingApplicationRegistrationStatus.READY;
        recordStatusChange(memberId, null);
    }

    public void cancelRegistrationReady(Long memberId) {
        RecruitingApplicationLifecyclePolicy.requireFinalPassed(status);
        requireRegistrationStatus(RecruitingApplicationRegistrationStatus.READY);
        this.registrationStatus = RecruitingApplicationRegistrationStatus.NOT_READY;
        recordStatusChange(memberId, null);
    }

    public void register(Long memberId) {
        RecruitingApplicationLifecyclePolicy.requireFinalPassed(status);
        requireRegistrationStatus(RecruitingApplicationRegistrationStatus.READY);
        this.registrationStatus = RecruitingApplicationRegistrationStatus.REGISTERED;
        recordStatusChange(memberId, null);
    }

    public boolean allowsReapplication() {
        return RecruitingApplicationLifecyclePolicy.allowsReapplication(status);
    }

    public boolean blocksReapplication() {
        return !allowsReapplication();
    }

    private static void validateRequired(
        RecruitingApplicationForm applicationForm,
        Long formResponseId,
        Long applicantMemberId,
        RecruitingApplicantProfile applicantProfile,
        String applicationKey
    ) {
        if (applicationForm == null
            || formResponseId == null
            || applicantMemberId == null
            || applicantProfile == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_REQUIRED_FIELD);
        }
        if (applicationKey == null || !APPLICATION_KEY_PATTERN.matcher(applicationKey).matches()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_KEY);
        }
    }

    public String getApplicantName() {
        return applicantProfile.getApplicantName();
    }

    public String getApplicantEmail() {
        return applicantProfile.getApplicantEmail();
    }

    public ChallengerTrack getFirstChoice() {
        return applicantProfile.getFirstChoice();
    }

    public ChallengerTrack getSecondChoice() {
        return applicantProfile.getSecondChoice();
    }

    private void transitionTo(RecruitingApplicationStatus targetStatus, Long memberId, String reason) {
        RecruitingApplicationLifecyclePolicy.requireTransition(status, targetStatus);
        this.status = targetStatus;
        recordStatusChange(memberId, reason);
    }

    private void requireRegistrationStatus(RecruitingApplicationRegistrationStatus expectedStatus) {
        if (registrationStatus != expectedStatus) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
    }

    private void recordStatusChange(Long memberId, String reason) {
        this.statusChangedMemberId = memberId;
        this.statusChangeReason = reason;
        this.statusChangedAt = Instant.now();
    }
}
