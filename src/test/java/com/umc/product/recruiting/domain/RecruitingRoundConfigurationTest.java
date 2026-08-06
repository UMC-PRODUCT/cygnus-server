package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingRoundConfigurationTest {

    private static final Instant DOCUMENT_START = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant DOCUMENT_END = Instant.parse("2026-08-08T00:00:00Z");
    private static final Instant DOCUMENT_RESULT = Instant.parse("2026-08-10T00:00:00Z");
    private static final Instant INTERVIEW_START = Instant.parse("2026-08-11T00:00:00Z");
    private static final Instant INTERVIEW_END = Instant.parse("2026-08-14T00:00:00Z");
    private static final Instant FINAL_RESULT = Instant.parse("2026-08-16T00:00:00Z");

    @Test
    @DisplayName("면접 차수 설정의 모든 필드를 보존한다")
    void configuredRoundWithInterview() {
        RecruitingRound round = RecruitingRound.createRegular(
            RecruitingSeason.create(1L, 10L),
            interviewConfiguration()
        );

        assertThat(round.getRecruitableTracks())
            .containsExactly(ChallengerTrack.PLAN, ChallengerTrack.DESIGN);
        assertThat(round.isSecondChoiceEnabled()).isTrue();
        assertThat(round.isInterviewRequired()).isTrue();
        assertThat(round.getAvailabilityFormId()).isEqualTo(100L);
        assertThat(round.getAvailabilityScheduleQuestionId()).isEqualTo(200L);
        assertThat(round.getAnnouncement()).isEqualTo("모집 안내");
        assertThat(round.getContactText()).isEqualTo("recruit@umc.test");
    }

    @Test
    @DisplayName("면접이 없는 차수는 면접 기간과 availability form 없이 생성한다")
    void configuredRoundWithoutInterview() {
        RecruitingRound round = RecruitingRound.createAdditional(
            RecruitingSeason.create(1L, 10L),
            2,
            noInterviewConfiguration(null)
        );

        assertThat(round.isInterviewRequired()).isFalse();
        assertThat(round.getInterviewStartAt()).isNull();
        assertThat(round.getInterviewEndAt()).isNull();
        assertThat(round.getAvailabilityFormId()).isNull();
    }

    @Test
    @DisplayName("면접이 없는 차수는 availability form을 설정할 수 없다")
    void configuredRoundWithoutInterviewRejectsAvailabilityForm() {
        assertThatThrownBy(() -> noInterviewConfiguration(100L, 200L))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
    }

    @Test
    @DisplayName("면접 가능 시간 매핑은 Form과 SCHEDULE question을 함께 설정해야 한다")
    void availabilityMappingRequiresFormAndScheduleQuestionPair() {
        assertInvalidSchedule(() -> interviewConfiguration(100L, null));
        assertInvalidSchedule(() -> interviewConfiguration(null, 200L));
    }

    @Test
    @DisplayName("면접 가능 시간 매핑 ID는 양수여야 한다")
    void availabilityMappingRequiresPositiveIds() {
        assertInvalidSchedule(() -> interviewConfiguration(0L, 200L));
        assertInvalidSchedule(() -> interviewConfiguration(100L, 0L));
        assertInvalidSchedule(() -> interviewConfiguration(-1L, 200L));
        assertInvalidSchedule(() -> interviewConfiguration(100L, -1L));
    }

    @Test
    @DisplayName("면접을 진행하는 DRAFT 차수는 면접 가능 시간 매핑 없이 생성할 수 있다")
    void draftInterviewRoundAllowsMissingAvailabilityMapping() {
        RecruitingRound round = RecruitingRound.createRegular(
            RecruitingSeason.create(1L, 10L),
            interviewConfiguration(null, null)
        );

        assertThat(round.getStatus().name()).isEqualTo("DRAFT");
        assertThat(round.getAvailabilityFormId()).isNull();
        assertThat(round.getAvailabilityScheduleQuestionId()).isNull();
    }

    @Test
    @DisplayName("면접을 진행하지 않는 차수는 면접 가능 시간 매핑을 설정할 수 없다")
    void noInterviewRoundRejectsAvailabilityMapping() {
        assertInvalidSchedule(() -> noInterviewConfiguration(100L, 200L));
    }

    @Test
    @DisplayName("모집 트랙은 비어 있을 수 없다")
    void configuredRoundRejectsEmptyTracks() {
        assertInvalidTracks(List.of());
    }

    @Test
    @DisplayName("모집 트랙은 중복될 수 없다")
    void configuredRoundRejectsDuplicateTracks() {
        assertInvalidTracks(List.of(ChallengerTrack.PLAN, ChallengerTrack.PLAN));
    }

    @Test
    @DisplayName("모집 트랙에는 null을 설정할 수 없다")
    void configuredRoundRejectsNullTrack() {
        assertInvalidTracks(Arrays.asList(ChallengerTrack.PLAN, null));
    }

    @Test
    @DisplayName("모집 트랙에는 INFRA_PLUS를 설정할 수 없다")
    void configuredRoundRejectsInfraPlusTrack() {
        assertInvalidTracks(List.of(ChallengerTrack.INFRA_PLUS));
    }

    private void assertInvalidTracks(List<ChallengerTrack> tracks) {
        assertThatThrownBy(() -> configurationWithTracks(tracks))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_TRACKS);
    }

    private RecruitingRoundConfiguration interviewConfiguration() {
        return interviewConfiguration(100L, 200L);
    }

    private RecruitingRoundConfiguration configurationWithTracks(List<ChallengerTrack> tracks) {
        return interviewConfiguration(tracks, 100L, 200L);
    }

    private RecruitingRoundConfiguration interviewConfiguration(Long availabilityFormId, Long scheduleQuestionId) {
        return interviewConfiguration(
            List.of(ChallengerTrack.PLAN, ChallengerTrack.DESIGN),
            availabilityFormId,
            scheduleQuestionId
        );
    }

    private RecruitingRoundConfiguration interviewConfiguration(
        List<ChallengerTrack> tracks,
        Long availabilityFormId,
        Long scheduleQuestionId
    ) {
        return RecruitingRoundConfiguration.of(
            tracks,
            true,
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            true,
            INTERVIEW_START,
            INTERVIEW_END,
            FINAL_RESULT,
            availabilityFormId,
            scheduleQuestionId,
            "모집 안내",
            "recruit@umc.test"
        );
    }

    private RecruitingRoundConfiguration noInterviewConfiguration(Long availabilityFormId) {
        return noInterviewConfiguration(availabilityFormId, null);
    }

    private RecruitingRoundConfiguration noInterviewConfiguration(Long availabilityFormId, Long scheduleQuestionId) {
        return RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
            false,
            DOCUMENT_START,
            DOCUMENT_END,
            DOCUMENT_RESULT,
            false,
            null,
            null,
            FINAL_RESULT,
            availabilityFormId,
            scheduleQuestionId,
            null,
            null
        );
    }

    private void assertInvalidSchedule(Runnable constructor) {
        assertThatThrownBy(constructor::run)
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
    }
}
