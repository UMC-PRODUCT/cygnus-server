package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingRoundScheduleValidationTest {

    private static final Instant DOCUMENT_START = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant DOCUMENT_END = Instant.parse("2026-08-08T00:00:00Z");
    private static final Instant DOCUMENT_RESULT = Instant.parse("2026-08-10T00:00:00Z");
    private static final Instant INTERVIEW_START = Instant.parse("2026-08-11T00:00:00Z");
    private static final Instant INTERVIEW_END = Instant.parse("2026-08-14T00:00:00Z");
    private static final Instant FINAL_RESULT = Instant.parse("2026-08-16T00:00:00Z");

    @Test
    @DisplayName("모집 차수는 필수 서류 일정이 모두 필요하다")
    void configuredRoundRejectsMissingRequiredSchedule() {
        assertInvalidSchedule(new Schedule(
            null,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            true,
            INTERVIEW_START,
            INTERVIEW_END,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("문서 접수 종료는 시작보다 빠를 수 없다")
    void configuredRoundRejectsMalformedDocumentWindow() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_END,
            DOCUMENT_START,
            DOCUMENT_RESULT,
            true,
            INTERVIEW_START,
            INTERVIEW_END,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("서류 결과 발표는 문서 접수 종료보다 빠를 수 없다")
    void configuredRoundRejectsDocumentResultBeforeDocumentEnd() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_START,
            true,
            INTERVIEW_START,
            INTERVIEW_END,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("면접 시작은 서류 결과 발표보다 빠를 수 없다")
    void configuredRoundRejectsInterviewBeforeDocumentResult() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            true,
            DOCUMENT_END,
            INTERVIEW_END,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("면접 필수 차수는 면접 시작 시각이 필요하다")
    void interviewRequiredRoundRejectsMissingInterviewStart() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            true,
            null,
            INTERVIEW_END,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("면접 필수 차수는 면접 종료 시각이 필요하다")
    void interviewRequiredRoundRejectsMissingInterviewEnd() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            true,
            INTERVIEW_START,
            null,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("면접 종료는 면접 시작보다 빠를 수 없다")
    void configuredRoundRejectsMalformedInterviewWindow() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            true,
            INTERVIEW_END,
            INTERVIEW_START,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("최종 결과 발표는 면접 종료보다 빠를 수 없다")
    void configuredRoundRejectsFinalResultBeforeInterviewEnd() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            true,
            INTERVIEW_START,
            INTERVIEW_END,
            INTERVIEW_START
        ));
    }

    @Test
    @DisplayName("면접 미진행 차수는 면접 시작 시각을 가질 수 없다")
    void noInterviewRoundRejectsInterviewStart() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            false,
            INTERVIEW_START,
            null,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("면접 미진행 차수는 면접 종료 시각을 가질 수 없다")
    void noInterviewRoundRejectsInterviewEnd() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            false,
            null,
            INTERVIEW_END,
            FINAL_RESULT
        ));
    }

    @Test
    @DisplayName("면접이 없으면 최종 결과 발표는 서류 결과 발표보다 빠를 수 없다")
    void noInterviewRoundRejectsFinalResultBeforeDocumentResult() {
        assertInvalidSchedule(new Schedule(
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            false,
            null,
            null,
            DOCUMENT_END
        ));
    }

    private void assertInvalidSchedule(Schedule schedule) {
        assertThatThrownBy(() -> RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN),
            false,
            schedule.documentStartAt(),
            schedule.documentEndAt(),
            schedule.documentResultPublishedAt(),
            schedule.interviewRequired(),
            schedule.interviewStartAt(),
            schedule.interviewEndAt(),
            schedule.finalResultPublishedAt(),
            null,
            null,
            null
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
    }

    private record Schedule(
        Instant documentStartAt,
        Instant documentEndAt,
        Instant documentResultPublishedAt,
        boolean interviewRequired,
        Instant interviewStartAt,
        Instant interviewEndAt,
        Instant finalResultPublishedAt
    ) {
    }
}
