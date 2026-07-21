package com.umc.product.recruiting.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;
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
    name = "recruiting_interview_schedule",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_interview_schedule_application",
        columnNames = "recruiting_application_id"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingInterviewSchedule extends BaseEntity {

    private static final int MAX_CONTACT_LENGTH = 2000;
    private static final int MAX_ERROR_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_id", nullable = false)
    private RecruitingApplication application;

    @Column(name = "availability_form_response_id")
    private Long availabilityFormResponseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingInterviewScheduleStatus status;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(length = 255)
    private String location;

    @Column(name = "contact_snapshot", nullable = false, length = MAX_CONTACT_LENGTH)
    private String contactSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_mail_status", nullable = false)
    private RecruitingMailDeliveryStatus requestMailStatus;

    @Column(name = "request_mail_attempts", nullable = false)
    private int requestMailAttempts;

    @Column(name = "request_mail_error", length = MAX_ERROR_LENGTH)
    private String requestMailError;

    @Column(name = "request_mail_sent_at")
    private Instant requestMailSentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_mail_status", nullable = false)
    private RecruitingMailDeliveryStatus confirmationMailStatus;

    @Column(name = "confirmation_mail_attempts", nullable = false)
    private int confirmationMailAttempts;

    @Column(name = "confirmation_mail_error", length = MAX_ERROR_LENGTH)
    private String confirmationMailError;

    @Column(name = "confirmation_mail_sent_at")
    private Instant confirmationMailSentAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingInterviewSchedule(RecruitingApplication application, String contactSnapshot) {
        validateApplication(application);
        validateContactSnapshot(contactSnapshot);
        this.application = application;
        this.contactSnapshot = contactSnapshot;
        this.status = RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED;
        this.requestMailStatus = RecruitingMailDeliveryStatus.PENDING;
        this.confirmationMailStatus = RecruitingMailDeliveryStatus.PENDING;
    }

    public static RecruitingInterviewSchedule requestAvailability(
        RecruitingApplication application,
        String contactSnapshot
    ) {
        return RecruitingInterviewSchedule.builder()
            .application(application)
            .contactSnapshot(contactSnapshot)
            .build();
    }

    public void submitAvailability(Long availabilityFormResponseId) {
        requireStatus(RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED);
        if (availabilityFormResponseId == null || availabilityFormResponseId <= 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE);
        }
        this.availabilityFormResponseId = availabilityFormResponseId;
        this.status = RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED;
    }

    public void confirm(Instant startsAt, Instant endsAt, String location, String contactSnapshot) {
        requireStatus(RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED);
        validatePeriod(startsAt, endsAt);
        validateLocation(location);
        validateContactSnapshot(contactSnapshot);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.contactSnapshot = contactSnapshot;
        this.status = RecruitingInterviewScheduleStatus.CONFIRMED;
        this.confirmationMailStatus = RecruitingMailDeliveryStatus.PENDING;
        this.confirmationMailError = null;
        this.confirmationMailSentAt = null;
    }

    public void cancel() {
        if (status == RecruitingInterviewScheduleStatus.CANCELLED) {
            return;
        }
        this.status = RecruitingInterviewScheduleStatus.CANCELLED;
    }

    public boolean isCancelled() {
        return status == RecruitingInterviewScheduleStatus.CANCELLED;
    }

    public void markRequestMailSent(Instant sentAt) {
        requireNotCancelled();
        validateSentAt(sentAt);
        requestMailAttempts++;
        requestMailStatus = RecruitingMailDeliveryStatus.SENT;
        requestMailError = null;
        requestMailSentAt = sentAt;
    }

    public void markRequestMailFailed(String error) {
        requireNotCancelled();
        validateError(error);
        requestMailAttempts++;
        requestMailStatus = RecruitingMailDeliveryStatus.FAILED;
        requestMailError = error;
        requestMailSentAt = null;
    }

    public void retryRequestMail() {
        requireNotCancelled();
        if (requestMailStatus != RecruitingMailDeliveryStatus.FAILED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_MAIL_STATE);
        }
        requestMailStatus = RecruitingMailDeliveryStatus.PENDING;
        requestMailError = null;
        requestMailSentAt = null;
    }

    public void markConfirmationMailSent(Instant sentAt) {
        requireStatus(RecruitingInterviewScheduleStatus.CONFIRMED);
        validateSentAt(sentAt);
        confirmationMailAttempts++;
        confirmationMailStatus = RecruitingMailDeliveryStatus.SENT;
        confirmationMailError = null;
        confirmationMailSentAt = sentAt;
    }

    public void markConfirmationMailFailed(String error) {
        requireStatus(RecruitingInterviewScheduleStatus.CONFIRMED);
        validateError(error);
        confirmationMailAttempts++;
        confirmationMailStatus = RecruitingMailDeliveryStatus.FAILED;
        confirmationMailError = error;
        confirmationMailSentAt = null;
    }

    private static void validateApplication(RecruitingApplication application) {
        if (application == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID);
        }
    }

    private static void validateContactSnapshot(String contactSnapshot) {
        if (contactSnapshot == null
            || contactSnapshot.isBlank()
            || contactSnapshot.length() > MAX_CONTACT_LENGTH) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_CONTACT);
        }
    }

    private static void validatePeriod(Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD);
        }
    }

    private static void validateLocation(String location) {
        if (location == null || location.isBlank() || location.length() > 255) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_LOCATION);
        }
    }

    private static void validateSentAt(Instant sentAt) {
        if (sentAt == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_MAIL_STATE);
        }
    }

    private static void validateError(String error) {
        if (error == null || error.isBlank() || error.length() > MAX_ERROR_LENGTH) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_MAIL_STATE);
        }
    }

    private void requireStatus(RecruitingInterviewScheduleStatus expected) {
        if (status != expected) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);
        }
    }

    private void requireNotCancelled() {
        if (isCancelled()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);
        }
    }
}
