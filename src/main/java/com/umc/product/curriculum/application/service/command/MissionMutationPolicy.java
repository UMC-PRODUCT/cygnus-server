package com.umc.product.curriculum.application.service.command;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupScheduleUseCase;
import com.umc.product.schedule.application.port.in.query.GetScheduleUseCase;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionMutationPolicy {

    private static final Duration FEEDBACK_EDIT_PERIOD = Duration.ofDays(14);
    private static final Duration WEEKLY_BEST_WITHDRAW_PERIOD = Duration.ofDays(7);
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final Clock clock;
    private final GetStudyGroupScheduleUseCase getStudyGroupScheduleUseCase;
    private final GetScheduleUseCase getScheduleUseCase;
    private final GetGisuUseCase getGisuUseCase;

    public void validateSubmissionCreate(WeeklyCurriculum weeklyCurriculum) {
        requireBefore(weeklyCurriculum.getEndsAt(), CurriculumErrorCode.SUBMISSION_PERIOD_ENDED);
    }

    public void validateSubmissionEdit(MissionSubmission submission) {
        if (submission.isWithdrawn()) {
            throw new CurriculumDomainException(CurriculumErrorCode.MISSION_SUBMISSION_ALREADY_WITHDRAWN);
        }

        WeeklyCurriculum weeklyCurriculum = submission.getOriginalWorkbookMission()
            .getOriginalWorkbook()
            .getWeeklyCurriculum();
        Long studyGroupId = submission.getChallengerWorkbook().getStudyGroupId();
        Instant deadline = resolveSubmissionEditDeadline(studyGroupId, weeklyCurriculum);
        requireBefore(deadline, CurriculumErrorCode.SUBMISSION_EDIT_PERIOD_ENDED);
    }

    public void validateFeedbackEdit(MissionFeedback feedback) {
        Instant deadline = feedback.getCreatedAt().plus(FEEDBACK_EDIT_PERIOD);
        requireBefore(deadline, CurriculumErrorCode.FEEDBACK_EDIT_PERIOD_ENDED);
    }

    public void validateFeedbackDelete(MissionFeedback feedback) {
        Long gisuId = feedback.getMissionSubmission()
            .getOriginalWorkbookMission()
            .getOriginalWorkbook()
            .getWeeklyCurriculum()
            .getCurriculum()
            .getGisuId();
        requireBefore(getGisuUseCase.getById(gisuId).endAt(), CurriculumErrorCode.FEEDBACK_DELETE_PERIOD_ENDED);
    }

    public void validateWeeklyBestWithdraw(WeeklyBestWorkbook weeklyBestWorkbook) {
        requireBefore(
            weeklyBestWorkbook.getWeeklyCurriculum().getEndsAt().plus(WEEKLY_BEST_WITHDRAW_PERIOD),
            CurriculumErrorCode.INVALID_WORKBOOK_STATUS
        );
    }

    public Instant now() {
        return clock.instant();
    }

    private Instant resolveSubmissionEditDeadline(
        Long studyGroupId,
        WeeklyCurriculum weeklyCurriculum
    ) {
        if (studyGroupId == null) {
            return weeklyCurriculum.getEndsAt();
        }
        return getStudyGroupScheduleUseCase
            .findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(studyGroupId, weeklyCurriculum.getId())
            .map(scheduleId -> resolveMappedScheduleDeadline(scheduleId, weeklyCurriculum))
            .orElse(weeklyCurriculum.getEndsAt());
    }

    private Instant resolveMappedScheduleDeadline(Long scheduleId, WeeklyCurriculum weeklyCurriculum) {
        try {
            return getScheduleUseCase.getScheduleBaseInfo(scheduleId)
                .startsAt()
                .atZone(KOREA_ZONE)
                .toLocalDate()
                .atStartOfDay(KOREA_ZONE)
                .toInstant();
        } catch (ScheduleDomainException e) {
            if (e.getBaseCode() == ScheduleErrorCode.SCHEDULE_NOT_FOUND) {
                return weeklyCurriculum.getEndsAt();
            }
            throw e;
        }
    }

    private void requireBefore(Instant deadline, CurriculumErrorCode errorCode) {
        if (!clock.instant().isBefore(deadline)) {
            throw new CurriculumDomainException(errorCode);
        }
    }
}
