package com.umc.product.recruiting.domain;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

final class RecruitingApplicationLifecyclePolicy {

    private RecruitingApplicationLifecyclePolicy() {
    }

    static void requireTransition(
        RecruitingApplicationStatus currentStatus,
        RecruitingApplicationStatus targetStatus
    ) {
        boolean allowed = switch (targetStatus) {
            case SUBMITTED -> currentStatus == RecruitingApplicationStatus.DRAFT;
            case CANCELLED -> currentStatus == RecruitingApplicationStatus.DRAFT
                || currentStatus == RecruitingApplicationStatus.SUBMITTED;
            case DOCUMENT_FAILED -> currentStatus == RecruitingApplicationStatus.SUBMITTED;
            case INTERVIEW_ASSIGNED -> currentStatus == RecruitingApplicationStatus.SUBMITTED;
            case INTERVIEW_SKIPPED -> currentStatus == RecruitingApplicationStatus.SUBMITTED
                || currentStatus == RecruitingApplicationStatus.INTERVIEW_ASSIGNED;
            case FINAL_PASSED, FINAL_FAILED -> currentStatus == RecruitingApplicationStatus.INTERVIEW_SKIPPED
                || currentStatus == RecruitingApplicationStatus.INTERVIEW_ASSIGNED;
            default -> false;
        };
        if (!allowed) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
    }

    static void requireFinalPassed(RecruitingApplicationStatus status) {
        if (status != RecruitingApplicationStatus.FINAL_PASSED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
    }

    static boolean allowsReapplication(RecruitingApplicationStatus status) {
        return status == RecruitingApplicationStatus.DOCUMENT_FAILED
            || status == RecruitingApplicationStatus.FINAL_FAILED
            || status == RecruitingApplicationStatus.CANCELLED;
    }
}
