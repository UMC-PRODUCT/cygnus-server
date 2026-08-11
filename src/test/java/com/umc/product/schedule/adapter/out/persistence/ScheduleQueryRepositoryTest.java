package com.umc.product.schedule.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(ScheduleQueryRepository.class)
@DisplayName("ScheduleQueryRepository")
class ScheduleQueryRepositoryTest {

    private static final Long MEMBER_ID = 100L;

    @Autowired
    ScheduleQueryRepository sut;

    @Autowired
    ScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    ScheduleParticipantJpaRepository scheduleParticipantJpaRepository;

    @ParameterizedTest(name = "{2} 조회")
    @CsvSource({
        "2026-06-01T00:00:00Z, 2026-06-30T23:59:59Z, 6월",
        "2026-07-01T00:00:00Z, 2026-07-31T23:59:59Z, 7월",
        "2026-08-01T00:00:00Z, 2026-08-31T23:59:59Z, 8월"
    })
    @DisplayName("여러 달에 걸친 일정은 걸쳐 있는 모든 달의 조회 결과에 포함된다")
    void findMySchedules_여러_달에_걸친_일정을_모든_달에서_조회한다(String from, String to, String month) {
        // given : 6/30 ~ 8/2 까지 진행되는 일정
        Schedule schedule = saveScheduleWithParticipant(
            "3개월에 걸친 일정",
            Instant.parse("2026-06-30T00:00:00Z"),
            Instant.parse("2026-08-02T00:00:00Z")
        );

        // when
        List<Schedule> results = sut.findMySchedules(
            MEMBER_ID, Instant.parse(from), Instant.parse(to), null
        );

        // then
        assertThat(results)
            .as("%s 조회 결과", month)
            .extracting(Schedule::getId)
            .containsExactly(schedule.getId());
    }

    @Test
    @DisplayName("조회 기간이 끝난 뒤에 시작하는 일정은 조회되지 않는다")
    void findMySchedules_조회_기간_이후에_시작하는_일정은_제외한다() {
        // given
        saveScheduleWithParticipant(
            "9월 일정",
            Instant.parse("2026-09-01T00:00:00Z"),
            Instant.parse("2026-09-01T02:00:00Z")
        );

        // when : 7월 조회
        List<Schedule> results = sut.findMySchedules(
            MEMBER_ID,
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-31T23:59:59Z"),
            null
        );

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("조회 기간이 시작하기 전에 끝난 일정은 조회되지 않는다")
    void findMySchedules_조회_기간_이전에_종료된_일정은_제외한다() {
        // given
        saveScheduleWithParticipant(
            "5월 일정",
            Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-05-01T02:00:00Z")
        );

        // when : 7월 조회
        List<Schedule> results = sut.findMySchedules(
            MEMBER_ID,
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-31T23:59:59Z"),
            null
        );

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("일정 종료 시각이 조회 시작 시각과 같으면 조회 결과에 포함된다")
    void findMySchedules_경계값인_종료_시각도_포함한다() {
        // given : 7월 조회 시작 시각에 정확히 끝나는 일정
        Schedule schedule = saveScheduleWithParticipant(
            "6월 말 ~ 7월 1일 0시 일정",
            Instant.parse("2026-06-30T22:00:00Z"),
            Instant.parse("2026-07-01T00:00:00Z")
        );

        // when
        List<Schedule> results = sut.findMySchedules(
            MEMBER_ID,
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-31T23:59:59Z"),
            null
        );

        // then
        assertThat(results)
            .extracting(Schedule::getId)
            .containsExactly(schedule.getId());
    }

    @Test
    @DisplayName("참여자가 아닌 사용자의 일정은 기간이 겹쳐도 조회되지 않는다")
    void findMySchedules_참여자가_아니면_제외한다() {
        // given
        saveScheduleWithParticipant(
            "3개월에 걸친 일정",
            Instant.parse("2026-06-30T00:00:00Z"),
            Instant.parse("2026-08-02T00:00:00Z")
        );

        // when : 다른 사용자로 7월 조회
        List<Schedule> results = sut.findMySchedules(
            MEMBER_ID + 1,
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-31T23:59:59Z"),
            null
        );

        // then
        assertThat(results).isEmpty();
    }

    private Schedule saveScheduleWithParticipant(String name, Instant startsAt, Instant endsAt) {
        Schedule schedule = scheduleJpaRepository.save(
            Schedule.builder()
                .name(name)
                .description("테스트 일정")
                .tags(Set.of(ScheduleTag.GENERAL))
                .authorMemberId(MEMBER_ID)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build()
        );

        scheduleParticipantJpaRepository.save(
            ScheduleParticipant.builder()
                .memberId(MEMBER_ID)
                .schedule(schedule)
                .build()
        );

        return schedule;
    }
}
