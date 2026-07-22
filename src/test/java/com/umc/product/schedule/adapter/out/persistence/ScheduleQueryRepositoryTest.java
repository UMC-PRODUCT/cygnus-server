package com.umc.product.schedule.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.member.domain.Member;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.ScheduleParticipantAttendance;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({ScheduleQueryRepository.class, ScheduleParticipantQueryRepository.class})
@DisplayName("Schedule QueryDSL repository")
class ScheduleQueryRepositoryTest {

    private static final Instant RANGE_FROM = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant RANGE_TO = Instant.parse("2030-01-31T23:59:59Z");

    @Autowired TestEntityManager em;
    @Autowired ScheduleQueryRepository scheduleRepository;
    @Autowired ScheduleParticipantQueryRepository participantRepository;

    Member firstMember;
    Member secondMember;
    Schedule inRange;
    Schedule outsidePending;
    Schedule outsideAbsent;
    Schedule noPolicy;

    @BeforeEach
    void setUp() {
        firstMember = em.persist(Member.create("첫 회원", "first", "schedule-first@example.com", null, null));
        secondMember = em.persist(Member.create("둘째 회원", "second", "schedule-second@example.com", null, null));

        inRange = em.persist(schedule("기간 내", RANGE_FROM.plus(5, ChronoUnit.DAYS), firstMember.getId(), true));
        outsidePending = em.persist(schedule("기간 밖 pending", RANGE_FROM.minus(10, ChronoUnit.DAYS),
            firstMember.getId(), true));
        outsideAbsent = em.persist(schedule("기간 밖 결석", RANGE_FROM.minus(20, ChronoUnit.DAYS),
            secondMember.getId(), true));
        noPolicy = em.persist(schedule("출석 없음", RANGE_FROM.plus(10, ChronoUnit.DAYS),
            firstMember.getId(), false));

        em.persist(participant(inRange, firstMember.getId(), AttendanceStatus.PRESENT));
        em.persist(participant(inRange, secondMember.getId(), AttendanceStatus.ABSENT));
        em.persist(participant(outsidePending, firstMember.getId(), AttendanceStatus.PRESENT_PENDING));
        em.persist(participant(outsideAbsent, firstMember.getId(), AttendanceStatus.ABSENT));
        em.persist(participant(noPolicy, firstMember.getId(), AttendanceStatus.PRESENT));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("내 일정은 참여·기간·출석 정책 조건과 시작 시각 정렬을 적용한다")
    void find_my_schedules_applies_filters_and_sorting() {
        assertThat(scheduleRepository.findMySchedules(
            firstMember.getId(), RANGE_FROM, RANGE_TO, true
        )).extracting(Schedule::getId).containsExactly(inRange.getId());

        assertThat(scheduleRepository.findMySchedules(
            firstMember.getId(), RANGE_FROM, RANGE_TO, false
        )).extracting(Schedule::getId).containsExactly(inRange.getId(), noPolicy.getId());
        assertThat(scheduleRepository.findMySchedules(
            Long.MAX_VALUE, RANGE_FROM, RANGE_TO, null
        )).isEmpty();
    }

    @Test
    @DisplayName("tag fetch 상세과 작성자 ID 목록은 존재·부재 계약을 지킨다")
    void details_and_author_ids() {
        assertThat(scheduleRepository.findByIdWithTags(inRange.getId()))
            .get().satisfies(schedule -> assertThat(schedule.getTags()).contains(ScheduleTag.GENERAL));
        assertThat(scheduleRepository.findByIdWithTags(Long.MAX_VALUE)).isEmpty();
        assertThat(scheduleRepository.findScheduleIdsByAuthor(firstMember.getId()))
            .contains(inRange.getId(), outsidePending.getId(), noPolicy.getId());
        assertThat(scheduleRepository.findScheduleIdsByAuthor(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    @DisplayName("운영진 조회는 빈 ID, 기간 조합, pending 포함, 명시 상태 필터를 구분한다")
    void admin_schedule_filters_cover_all_combinations() {
        Set<Long> ids = Set.of(inRange.getId(), outsidePending.getId(), outsideAbsent.getId(), noPolicy.getId());
        assertThat(scheduleRepository.findAdminSchedulesByRole(Set.of(), null, null, null)).isEmpty();

        assertThat(scheduleRepository.findAdminSchedulesByRole(
            ids, RANGE_FROM, RANGE_TO, null
        )).extracting(Schedule::getId).containsExactlyInAnyOrder(inRange.getId(), outsidePending.getId());

        assertThat(scheduleRepository.findAdminSchedulesByRole(
            ids, RANGE_FROM, null, null
        )).extracting(Schedule::getId).contains(inRange.getId(), outsidePending.getId());

        assertThat(scheduleRepository.findAdminSchedulesByRole(
            ids, null, RANGE_TO, null
        )).extracting(Schedule::getId)
            .contains(inRange.getId(), outsidePending.getId(), outsideAbsent.getId());

        assertThat(scheduleRepository.findAdminSchedulesByRole(
            ids, RANGE_FROM, RANGE_TO, AttendanceStatus.ABSENT
        )).extracting(Schedule::getId).containsExactly(inRange.getId());

        assertThat(scheduleRepository.findAdminSchedulesByRole(
            ids, null, null, AttendanceStatus.ABSENT
        )).extracting(Schedule::getId).containsExactlyInAnyOrder(inRange.getId(), outsideAbsent.getId());

        assertThat(scheduleRepository.findAdminSchedulesByRole(
            ids, null, null, null
        )).extracting(Schedule::getId).containsExactly(outsidePending.getId());
    }

    @Test
    @DisplayName("참여자 상세 조회는 빈 입력·단건·batch·상태 필터와 left join을 처리한다")
    void participant_detail_filters() {
        assertThat(participantRepository.findParticipantDetailsByScheduleIds(null)).isEmpty();
        assertThat(participantRepository.findParticipantDetailsByScheduleIds(List.of())).isEmpty();
        assertThat(participantRepository.findParticipantDetailsByScheduleId(null)).isEmpty();

        assertThat(participantRepository.findParticipantDetailsByScheduleIds(
            List.of(inRange.getId(), outsidePending.getId())
        )).hasSize(3);
        assertThat(participantRepository.findParticipantDetailsByScheduleId(inRange.getId()))
            .hasSize(2)
            .allSatisfy(detail -> assertThat(detail.schoolId()).isNull());
        assertThat(participantRepository.findParticipantDetailsByScheduleIdAndStatus(
            inRange.getId(), AttendanceStatus.ABSENT
        )).singleElement().satisfies(detail ->
            assertThat(detail.memberId()).isEqualTo(secondMember.getId())
        );
        assertThat(participantRepository.findParticipantDetailsByScheduleIdAndStatus(
            inRange.getId(), null
        )).hasSize(2);
    }

    @Test
    @DisplayName("참여자·일정 ID와 출석 기록 존재 여부를 조회하고 schedule 단위로 벌크 삭제한다")
    void participant_ids_exists_and_bulk_delete() {
        assertThat(participantRepository.findMemberIdsByScheduleId(inRange.getId()))
            .containsExactlyInAnyOrder(firstMember.getId(), secondMember.getId());
        assertThat(participantRepository.findScheduleIdsByMemberId(firstMember.getId()))
            .contains(inRange.getId(), outsidePending.getId(), outsideAbsent.getId(), noPolicy.getId());
        assertThat(participantRepository.existsAttendanceStatusByScheduleId(inRange.getId())).isTrue();
        assertThat(participantRepository.existsAttendanceStatusByScheduleId(Long.MAX_VALUE)).isFalse();

        long deleted = participantRepository.deleteByScheduleId(inRange.getId());
        em.flush();
        em.clear();

        assertThat(deleted).isEqualTo(2L);
        assertThat(participantRepository.findMemberIdsByScheduleId(inRange.getId())).isEmpty();
    }

    private Schedule schedule(String name, Instant startsAt, Long authorId, boolean attendanceRequired) {
        Instant endsAt = startsAt.plus(2, ChronoUnit.HOURS);
        return Schedule.builder()
            .name(name)
            .description("설명")
            .tags(Set.of(ScheduleTag.GENERAL))
            .authorMemberId(authorId)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .policy(attendanceRequired ? Schedule.createAttendancePolicy(
                startsAt.minus(10, ChronoUnit.MINUTES),
                startsAt.plus(10, ChronoUnit.MINUTES),
                startsAt.plus(20, ChronoUnit.MINUTES),
                startsAt,
                endsAt
            ) : null)
            .build();
    }

    private ScheduleParticipant participant(Schedule schedule, Long memberId, AttendanceStatus status) {
        return ScheduleParticipant.builder()
            .schedule(schedule)
            .memberId(memberId)
            .attendance(ScheduleParticipantAttendance.create(null, true, "사유", status))
            .build();
    }
}
