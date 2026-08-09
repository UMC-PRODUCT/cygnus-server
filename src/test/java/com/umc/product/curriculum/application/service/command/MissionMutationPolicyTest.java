package com.umc.product.curriculum.application.service.command;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupScheduleUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.schedule.application.port.in.query.GetScheduleUseCase;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleBaseInfo;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

@ExtendWith(MockitoExtension.class)
class MissionMutationPolicyTest {

    private static final Instant WEEK_END = Instant.parse("2026-07-31T00:00:00Z");

    @Mock
    private GetStudyGroupScheduleUseCase getStudyGroupScheduleUseCase;
    @Mock
    private GetScheduleUseCase getScheduleUseCase;
    @Mock
    private GetGisuUseCase getGisuUseCase;

    @Test
    @DisplayName("제출 종료 시각 직전에는 생성할 수 있지만 정각부터 거부한다")
    void submissionCreate_exclusiveDeadline() {
        WeeklyCurriculum weekly = weekly();

        assertThatCode(() -> policy(WEEK_END.minusNanos(1)).validateSubmissionCreate(weekly))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy(WEEK_END).validateSubmissionCreate(weekly))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.SUBMISSION_PERIOD_ENDED);
    }

    @Test
    @DisplayName("일정이 있으면 KST 일정 시작일 00시부터 제출 수정을 거부한다")
    void submissionEdit_usesScheduleDateInKst() {
        MissionSubmission submission = submission();
        Instant scheduleStart = Instant.parse("2026-07-10T09:00:00Z");
        Instant deadline = Instant.parse("2026-07-09T15:00:00Z");
        given(getStudyGroupScheduleUseCase.findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(10L, 20L))
            .willReturn(Optional.of(30L));
        given(getScheduleUseCase.getScheduleBaseInfo(30L)).willReturn(schedule(scheduleStart));

        assertThatCode(() -> policy(deadline.minusNanos(1)).validateSubmissionEdit(submission))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy(deadline).validateSubmissionEdit(submission))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.SUBMISSION_EDIT_PERIOD_ENDED);
    }

    @Test
    @DisplayName("일정 매핑이 없으면 주차 종료 시각을 제출 수정 마감으로 사용한다")
    void submissionEditFallsBackToWeeklyEnd() {
        MissionSubmission submission = submission();
        given(getStudyGroupScheduleUseCase.findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(10L, 20L))
            .willReturn(Optional.empty());

        assertThatCode(() -> policy(WEEK_END.minusNanos(1)).validateSubmissionEdit(submission))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy(WEEK_END).validateSubmissionEdit(submission))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.SUBMISSION_EDIT_PERIOD_ENDED);
    }

    @Test
    @DisplayName("일정 매핑 대상이 삭제되었으면 주차 종료 시각을 제출 수정 마감으로 사용한다")
    void submissionEditFallsBackWhenMappedScheduleWasDeleted() {
        MissionSubmission submission = submission();
        given(getStudyGroupScheduleUseCase.findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(10L, 20L))
            .willReturn(Optional.of(30L));
        given(getScheduleUseCase.getScheduleBaseInfo(30L))
            .willThrow(new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        assertThatCode(() -> policy(WEEK_END.minusNanos(1)).validateSubmissionEdit(submission))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy(WEEK_END).validateSubmissionEdit(submission))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.SUBMISSION_EDIT_PERIOD_ENDED);
    }

    @Test
    @DisplayName("이미 철회된 제출은 수정 마감과 관계없이 거부한다")
    void withdrawnSubmissionCannotBeEdited() {
        MissionSubmission submission = submission();
        submission.withdraw(WEEK_END.minusSeconds(1));

        assertThatThrownBy(() -> policy(WEEK_END.minusSeconds(2)).validateSubmissionEdit(submission))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.MISSION_SUBMISSION_ALREADY_WITHDRAWN);
    }

    @Test
    @DisplayName("피드백은 작성 후 14일 정각부터 수정할 수 없다")
    void feedbackEdit_exclusiveDeadline() {
        MissionFeedback feedback = MissionFeedback.create(submission(), 2L, "피드백", FeedbackResult.PASS);
        Instant createdAt = Instant.parse("2026-07-01T00:00:00Z");
        ReflectionTestUtils.setField(feedback, "createdAt", createdAt);

        assertThatThrownBy(() -> policy(createdAt.plusSeconds(14 * 24 * 60 * 60))
            .validateFeedbackEdit(feedback))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.FEEDBACK_EDIT_PERIOD_ENDED);
    }

    @Test
    @DisplayName("피드백은 작성 후 14일 직전까지 수정할 수 있지만 직후에는 거부한다")
    void feedbackEditBeforeAndAfterDeadline() {
        MissionFeedback feedback = MissionFeedback.create(submission(), 2L, "피드백", FeedbackResult.PASS);
        Instant createdAt = Instant.parse("2026-07-01T00:00:00Z");
        Instant deadline = createdAt.plusSeconds(14 * 24 * 60 * 60);
        ReflectionTestUtils.setField(feedback, "createdAt", createdAt);

        assertThatCode(() -> policy(deadline.minusNanos(1)).validateFeedbackEdit(feedback))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy(deadline.plusNanos(1)).validateFeedbackEdit(feedback))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.FEEDBACK_EDIT_PERIOD_ENDED);
    }

    @Test
    @DisplayName("기수 종료 시각부터 피드백을 삭제할 수 없다")
    void feedbackDelete_exclusiveGisuEnd() {
        MissionFeedback feedback = MissionFeedback.create(submission(), 2L, "피드백", FeedbackResult.PASS);
        Instant gisuEnd = Instant.parse("2026-08-01T00:00:00Z");
        given(getGisuUseCase.getById(9L)).willReturn(new GisuInfo(9L, 9L, Instant.EPOCH, gisuEnd, true));

        assertThatThrownBy(() -> policy(gisuEnd).validateFeedbackDelete(feedback))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.FEEDBACK_DELETE_PERIOD_ENDED);
    }

    @Test
    @DisplayName("피드백은 기수 종료 직전까지 삭제할 수 있지만 직후에는 거부한다")
    void feedbackDeleteBeforeAndAfterGisuEnd() {
        MissionFeedback feedback = MissionFeedback.create(submission(), 2L, "피드백", FeedbackResult.PASS);
        Instant gisuEnd = Instant.parse("2026-08-01T00:00:00Z");
        given(getGisuUseCase.getById(9L)).willReturn(new GisuInfo(9L, 9L, Instant.EPOCH, gisuEnd, true));

        assertThatCode(() -> policy(gisuEnd.minusNanos(1)).validateFeedbackDelete(feedback))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy(gisuEnd.plusNanos(1)).validateFeedbackDelete(feedback))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.FEEDBACK_DELETE_PERIOD_ENDED);
    }

    @Test
    @DisplayName("베스트 선정은 주차 종료 후 7일 정각부터 철회할 수 없다")
    void weeklyBestWithdraw_exclusiveDeadline() {
        WeeklyCurriculum weekly = weekly();
        WeeklyBestWorkbook best = WeeklyBestWorkbook.create(weekly, 30L, 10L, "선정", 50L);
        Instant deadline = WEEK_END.plusSeconds(7 * 24 * 60 * 60);

        assertThatCode(() -> policy(deadline.minusNanos(1)).validateWeeklyBestWithdraw(best))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy(deadline).validateWeeklyBestWithdraw(best))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.INVALID_WORKBOOK_STATUS);
    }

    private MissionMutationPolicy policy(Instant now) {
        return new MissionMutationPolicy(
            Clock.fixed(now, ZoneOffset.UTC),
            getStudyGroupScheduleUseCase,
            getScheduleUseCase,
            getGisuUseCase
        );
    }

    private MissionSubmission submission() {
        WeeklyCurriculum weekly = weekly();
        ReflectionTestUtils.setField(weekly, "id", 20L);
        OriginalWorkbook original = OriginalWorkbook.createAsDraft(
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        );
        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            original, "미션", null, MissionType.MEMO, true
        );
        return MissionSubmission.create(mission, ChallengerWorkbook.create(original, 1L, 10L), "제출");
    }

    private WeeklyCurriculum weekly() {
        return WeeklyCurriculum.create(
            Curriculum.create(9L, ChallengerPart.SPRINGBOOT, "커리큘럼"),
            1L, false, "1주차", Instant.parse("2026-07-01T00:00:00Z"), WEEK_END
        );
    }

    private ScheduleBaseInfo schedule(Instant startsAt) {
        return new ScheduleBaseInfo(
            30L, "스터디", null, Set.of(), 1L, startsAt, startsAt.plusSeconds(3600),
            true, null, false, null
        );
    }
}
