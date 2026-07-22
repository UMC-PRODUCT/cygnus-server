package com.umc.product.schedule.adapter.out.persistence;

import static com.umc.product.support.fixture.ScheduleUnitFixture.participant;
import static com.umc.product.support.fixture.ScheduleUnitFixture.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.schedule.application.port.out.dto.ScheduleParticipantDetailDto;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.enums.AttendanceStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("Schedule persistence adapter")
class SchedulePersistenceAdapterUnitTest {

    @Mock ScheduleJpaRepository scheduleJpaRepository;
    @Mock ScheduleQueryRepository scheduleQueryRepository;
    @Mock ScheduleParticipantJpaRepository participantJpaRepository;
    @Mock ScheduleParticipantQueryRepository participantQueryRepository;

    @Test
    @DisplayName("Schedule load·save·delete port를 올바른 repository에 위임한다")
    void schedule_adapter_delegates_all_operations() {
        Schedule schedule = schedule();
        Instant from = Instant.parse("2029-01-01T00:00:00Z");
        Instant to = Instant.parse("2031-01-01T00:00:00Z");
        Set<Long> ids = Set.of(10L);
        given(scheduleJpaRepository.findById(10L)).willReturn(Optional.of(schedule));
        given(scheduleJpaRepository.existsById(10L)).willReturn(true);
        given(scheduleQueryRepository.findMySchedules(1L, from, to, true)).willReturn(List.of(schedule));
        given(scheduleQueryRepository.findByIdWithTags(10L)).willReturn(Optional.of(schedule));
        given(scheduleQueryRepository.findAdminSchedulesByRole(ids, from, to, AttendanceStatus.PRESENT))
            .willReturn(List.of(schedule));
        given(scheduleQueryRepository.findScheduleIdsByAuthor(1L)).willReturn(ids);
        given(scheduleJpaRepository.save(schedule)).willReturn(schedule);
        SchedulePersistenceAdapter sut = new SchedulePersistenceAdapter(
            scheduleJpaRepository, scheduleQueryRepository
        );

        assertThat(sut.findById(10L)).contains(schedule);
        assertThat(sut.existsById(10L)).isTrue();
        assertThat(sut.findMySchedules(1L, from, to, true)).containsExactly(schedule);
        assertThat(sut.findByIdWithTags(10L)).contains(schedule);
        assertThat(sut.findAdminSchedulesByRole(ids, from, to, AttendanceStatus.PRESENT))
            .containsExactly(schedule);
        assertThat(sut.findScheduleIdsByAuthor(1L)).isEqualTo(ids);
        assertThat(sut.save(schedule)).isSameAs(schedule);
        sut.delete(10L);
        then(scheduleJpaRepository).should().deleteById(10L);
    }

    @Test
    @DisplayName("ScheduleParticipant의 모든 load·save·delete port를 위임한다")
    void participant_adapter_delegates_all_operations() {
        Schedule schedule = schedule();
        ScheduleParticipant participant = participant(schedule);
        List<ScheduleParticipant> participants = List.of(participant);
        ScheduleParticipantDetailDto detail = new ScheduleParticipantDetailDto(
            10L, 1L, "회원", "닉네임", 1L, "학교", null, AttendanceStatus.PRESENT, null, true
        );
        given(participantJpaRepository.save(participant)).willReturn(participant);
        given(participantJpaRepository.saveAll(participants)).willReturn(participants);
        given(participantJpaRepository.findAllByScheduleId(10L)).willReturn(participants);
        given(participantJpaRepository.findByScheduleIdAndMemberId(10L, 1L))
            .willReturn(Optional.of(participant));
        given(participantQueryRepository.findParticipantDetailsByScheduleIds(List.of(10L)))
            .willReturn(List.of(detail));
        given(participantQueryRepository.findParticipantDetailsByScheduleId(10L)).willReturn(List.of(detail));
        given(participantQueryRepository.findParticipantDetailsByScheduleIdAndStatus(
            10L, AttendanceStatus.PRESENT)).willReturn(List.of(detail));
        given(participantQueryRepository.findMemberIdsByScheduleId(10L)).willReturn(Set.of(1L));
        given(participantQueryRepository.findScheduleIdsByMemberId(1L)).willReturn(Set.of(10L));
        given(participantQueryRepository.existsAttendanceStatusByScheduleId(10L)).willReturn(true);
        ScheduleParticipantPersistenceAdapter sut = new ScheduleParticipantPersistenceAdapter(
            participantJpaRepository, participantQueryRepository
        );

        assertThat(sut.save(participant)).isSameAs(participant);
        assertThat(sut.saveAll(participants)).containsExactly(participant);
        sut.deleteAll(participants);
        sut.deleteByScheduleId(10L);
        assertThat(sut.findAllByScheduleId(10L)).containsExactly(participant);
        assertThat(sut.findByScheduleIdAndMemberId(10L, 1L)).contains(participant);
        assertThat(sut.findParticipantDetailsByScheduleIds(List.of(10L))).containsExactly(detail);
        assertThat(sut.findParticipantDetailsByScheduleId(10L)).containsExactly(detail);
        assertThat(sut.findParticipantDetailsByScheduleIdAndStatus(10L, AttendanceStatus.PRESENT))
            .containsExactly(detail);
        assertThat(sut.findMemberIdsByScheduleId(10L)).containsExactly(1L);
        assertThat(sut.findScheduleIdsByMemberId(1L)).containsExactly(10L);
        assertThat(sut.existsAttendanceStatusByScheduleId(10L)).isTrue();
        then(participantJpaRepository).should().deleteAll(participants);
        then(participantQueryRepository).should().deleteByScheduleId(10L);
    }
}
