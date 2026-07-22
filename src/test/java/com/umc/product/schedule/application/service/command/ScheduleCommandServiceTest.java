package com.umc.product.schedule.application.service.command;

import static com.umc.product.support.fixture.ScheduleUnitFixture.DEFAULT_ENDS_AT;
import static com.umc.product.support.fixture.ScheduleUnitFixture.DEFAULT_STARTS_AT;
import static com.umc.product.support.fixture.ScheduleUnitFixture.participant;
import static com.umc.product.support.fixture.ScheduleUnitFixture.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfoWithStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.schedule.application.port.in.command.dto.CreateScheduleCommand;
import com.umc.product.schedule.application.port.in.command.dto.EditScheduleCommand;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleCapabilitiesInfo;
import com.umc.product.schedule.application.port.out.DeleteScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.DeleteSchedulePort;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.application.port.out.SaveScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.SaveSchedulePort;
import com.umc.product.schedule.application.service.query.ScheduleCapabilitiesService;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleCommandService")
class ScheduleCommandServiceTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    @Mock SaveSchedulePort saveSchedulePort;
    @Mock LoadSchedulePort loadSchedulePort;
    @Mock DeleteSchedulePort deleteSchedulePort;
    @Mock SaveScheduleParticipantPort saveScheduleParticipantPort;
    @Mock DeleteScheduleParticipantPort deleteScheduleParticipantPort;
    @Mock LoadScheduleParticipantPort loadScheduleParticipantPort;
    @Mock ScheduleCapabilitiesService capabilitiesService;
    @Mock GetChallengerUseCase getChallengerUseCase;
    @Mock GetGisuUseCase getGisuUseCase;
    @Mock GetMemberUseCase getMemberUseCase;

    ScheduleCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new ScheduleCommandService(
            saveSchedulePort, loadSchedulePort, deleteSchedulePort,
            saveScheduleParticipantPort, deleteScheduleParticipantPort, loadScheduleParticipantPort,
            capabilitiesService, getChallengerUseCase, getGisuUseCase, getMemberUseCase
        );
    }

    @Test
    @DisplayName("유효한 일정과 참여자를 저장하고 생성 ID를 반환한다")
    void creates_schedule_with_participants() {
        CreateScheduleCommand command = createCommand(Set.of(2L, 3L), null);
        prepareCreate(ScheduleCapabilitiesInfo.forCentralCore(), command.participantMemberIds().size());
        given(saveSchedulePort.save(any(Schedule.class))).willAnswer(invocation -> {
            Schedule saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", SCHEDULE_ID);
            return saved;
        });

        Long result = sut.create(command);

        assertThat(result).isEqualTo(SCHEDULE_ID);
        ArgumentCaptor<List<ScheduleParticipant>> captor = ArgumentCaptor.forClass(List.class);
        then(saveScheduleParticipantPort).should().saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(ScheduleParticipant::getMemberId)
            .containsExactlyInAnyOrder(2L, 3L);
        assertThat(captor.getValue()).allSatisfy(p -> assertThat(p.getSchedule().getId()).isEqualTo(SCHEDULE_ID));
    }

    @Test
    @DisplayName("생성 capability·최대 인원·출석 일정 권한을 각각 검증한다")
    void validates_create_capabilities() {
        CreateScheduleCommand normal = createCommand(Set.of(2L), null);
        given(capabilitiesService.getCapabilities(AUTHOR_ID)).willReturn(ScheduleCapabilitiesInfo.notAllowed());
        assertError(() -> sut.create(normal), ScheduleErrorCode.CANNOT_CREATE_SCHEDULE);

        given(capabilitiesService.getCapabilities(AUTHOR_ID)).willReturn(ScheduleCapabilitiesInfo.forChallenger());
        CreateScheduleCommand tooMany = createCommand(
            java.util.stream.LongStream.rangeClosed(1, 51).boxed().collect(java.util.stream.Collectors.toSet()), null
        );
        assertError(() -> sut.create(tooMany), ScheduleErrorCode.EXCEEDED_MAX_PARTICIPANTS);

        CreateScheduleCommand attendanceRequired = createCommand(
            Set.of(2L),
            CreateScheduleCommand.AttendancePolicyInfo.builder()
                .checkInStartAt(DEFAULT_STARTS_AT.minus(10, ChronoUnit.MINUTES))
                .onTimeEndAt(DEFAULT_STARTS_AT.plus(10, ChronoUnit.MINUTES))
                .lateEndAt(DEFAULT_STARTS_AT.plus(20, ChronoUnit.MINUTES))
                .build()
        );
        assertError(
            () -> sut.create(attendanceRequired),
            ScheduleErrorCode.CANNOT_CREATE_ATTENDANCE_REQUIRED_SCHEDULE
        );
    }

    @Test
    @DisplayName("활성 기수 밖의 일정과 존재하지 않는 초대 회원을 거부한다")
    void validates_active_gisu_and_invited_members() {
        CreateScheduleCommand command = createCommand(Set.of(2L, 3L), null);
        given(capabilitiesService.getCapabilities(AUTHOR_ID)).willReturn(ScheduleCapabilitiesInfo.forCentralCore());
        given(getChallengerUseCase.getLatestActiveChallengerByMemberId(AUTHOR_ID))
            .willReturn(challenger(AUTHOR_ID));
        given(getGisuUseCase.getActiveGisu()).willReturn(new GisuInfo(
            1L, 1L, DEFAULT_STARTS_AT.plusSeconds(1), DEFAULT_ENDS_AT, true
        ));
        assertError(() -> sut.create(command), ScheduleErrorCode.NOT_ACTIVE_GISU_SCHEDULE);

        given(getGisuUseCase.getActiveGisu()).willReturn(activeGisu());
        given(getMemberUseCase.countMembersByIds(command.participantMemberIds())).willReturn(1L);
        assertError(() -> sut.create(command), ScheduleErrorCode.INVALID_MEMBER_INVITE);
    }

    @Test
    @DisplayName("기본 필드 수정은 capability 조회 없이 저장한다")
    void updates_basic_fields_without_capability_lookup() {
        Schedule schedule = identifiedSchedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        EditScheduleCommand command = EditScheduleCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .name("수정")
            .description("수정 설명")
            .tags(Set.of(ScheduleTag.STUDY))
            .build();

        Long result = sut.update(command);

        assertThat(result).isEqualTo(SCHEDULE_ID);
        assertThat(schedule.getName()).isEqualTo("수정");
        then(capabilitiesService).shouldHaveNoInteractions();
        then(saveSchedulePort).should().save(schedule);
    }

    @Test
    @DisplayName("온라인·출석 불필요 전환은 장소와 정책을 제거한다")
    void converts_to_online_without_attendance() {
        Schedule schedule = identifiedSchedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        EditScheduleCommand command = EditScheduleCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .isOnline(true)
            .isAttendanceRequired(false)
            .build();

        sut.update(command);

        assertThat(schedule.getLocation()).isNull();
        assertThat(schedule.getLocationName()).isNull();
        assertThat(schedule.getPolicy()).isNull();
    }

    @Test
    @DisplayName("대면 위치·시간·출석 정책을 함께 변경한다")
    void updates_location_time_and_policy() {
        Schedule schedule = identifiedSchedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        Instant startsAt = DEFAULT_STARTS_AT.plus(1, ChronoUnit.HOURS);
        Instant endsAt = DEFAULT_ENDS_AT.plus(1, ChronoUnit.HOURS);
        EditScheduleCommand command = EditScheduleCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .location(EditScheduleCommand.LocationInfo.builder()
                .latitude(36.0).longitude(128.0).locationName("새 장소").build())
            .attendancePolicy(EditScheduleCommand.AttendancePolicyInfo.builder()
                .checkInStartAt(startsAt.minus(5, ChronoUnit.MINUTES))
                .onTimeEndAt(startsAt.plus(5, ChronoUnit.MINUTES))
                .lateEndAt(startsAt.plus(10, ChronoUnit.MINUTES))
                .build())
            .build();

        sut.update(command);

        assertThat(schedule.getStartsAt()).isEqualTo(startsAt);
        assertThat(schedule.getLocation().getY()).isEqualTo(36.0);
        assertThat(schedule.getLocationName()).isEqualTo("새 장소");
        assertThat(schedule.getPolicy().getEarlyCheckInMinutes()).isEqualTo(5L);
    }

    @Test
    @DisplayName("참여자 diff는 삭제와 추가만 수행하고 새 회원 존재 여부를 검증한다")
    void updates_only_participant_diff() {
        Schedule schedule = identifiedSchedule();
        ScheduleParticipant retained = participant(schedule);
        ScheduleParticipant removed = ScheduleParticipant.builder().memberId(3L).schedule(schedule).build();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(capabilitiesService.getCapabilities(AUTHOR_ID)).willReturn(ScheduleCapabilitiesInfo.forCentralCore());
        given(loadScheduleParticipantPort.findMemberIdsByScheduleId(SCHEDULE_ID)).willReturn(Set.of(2L, 3L));
        given(loadScheduleParticipantPort.findAllByScheduleId(SCHEDULE_ID)).willReturn(List.of(retained, removed));
        given(getMemberUseCase.countMembersByIds(Set.of(4L))).willReturn(1L);
        EditScheduleCommand command = EditScheduleCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .participantMemberIds(Set.of(2L, 4L))
            .build();

        sut.update(command);

        then(deleteScheduleParticipantPort).should().deleteAll(List.of(removed));
        ArgumentCaptor<List<ScheduleParticipant>> added = ArgumentCaptor.forClass(List.class);
        then(saveScheduleParticipantPort).should().saveAll(added.capture());
        assertThat(added.getValue()).extracting(ScheduleParticipant::getMemberId).containsExactly(4L);
    }

    @Test
    @DisplayName("동일 참여자 집합이면 write를 생략하고 잘못된 신규 회원은 거부한다")
    void participant_update_noop_and_invalid_member() {
        Schedule schedule = identifiedSchedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(capabilitiesService.getCapabilities(AUTHOR_ID)).willReturn(ScheduleCapabilitiesInfo.forCentralCore());
        given(loadScheduleParticipantPort.findMemberIdsByScheduleId(SCHEDULE_ID)).willReturn(Set.of(2L));
        sut.update(EditScheduleCommand.builder()
            .scheduleId(SCHEDULE_ID).participantMemberIds(Set.of(2L)).build());
        then(saveScheduleParticipantPort).shouldHaveNoInteractions();
        then(deleteScheduleParticipantPort).shouldHaveNoInteractions();

        given(loadScheduleParticipantPort.findMemberIdsByScheduleId(SCHEDULE_ID)).willReturn(Set.of(2L));
        given(loadScheduleParticipantPort.findAllByScheduleId(SCHEDULE_ID))
            .willReturn(List.of(ScheduleParticipant.builder().memberId(2L).schedule(schedule).build()));
        given(getMemberUseCase.countMembersByIds(Set.of(99L))).willReturn(0L);
        assertError(
            () -> sut.update(EditScheduleCommand.builder()
                .scheduleId(SCHEDULE_ID).participantMemberIds(Set.of(2L, 99L)).build()),
            ScheduleErrorCode.INVALID_MEMBER_INVITE
        );
    }

    @Test
    @DisplayName("수정은 not-found·시작 이후·권한별 최대값을 검증한다")
    void validates_update_preconditions() {
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.empty());
        assertError(() -> sut.update(EditScheduleCommand.builder().scheduleId(SCHEDULE_ID).build()),
            ScheduleErrorCode.SCHEDULE_NOT_FOUND);

        Schedule started = identifiedSchedule();
        ReflectionTestUtils.setField(started, "startsAt", Instant.now().minusSeconds(1));
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(started));
        assertError(() -> sut.update(EditScheduleCommand.builder().scheduleId(SCHEDULE_ID).build()),
            ScheduleErrorCode.STARTED_SCHEDULE_CANT_BE_EDITED);

        Schedule future = identifiedSchedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(future));
        given(capabilitiesService.getCapabilities(AUTHOR_ID)).willReturn(ScheduleCapabilitiesInfo.forChallenger());
        assertError(() -> sut.update(EditScheduleCommand.builder()
            .scheduleId(SCHEDULE_ID).isAttendanceRequired(true)
            .attendancePolicy(EditScheduleCommand.AttendancePolicyInfo.builder().build()).build()),
            ScheduleErrorCode.CANNOT_CREATE_ATTENDANCE_REQUIRED_SCHEDULE);

        Set<Long> tooMany = java.util.stream.LongStream.rangeClosed(1, 51).boxed()
            .collect(java.util.stream.Collectors.toSet());
        assertError(() -> sut.update(EditScheduleCommand.builder()
            .scheduleId(SCHEDULE_ID).participantMemberIds(tooMany).build()),
            ScheduleErrorCode.EXCEEDED_MAX_PARTICIPANTS);
    }

    @Test
    @DisplayName("일반 삭제는 출석 기록을 보호하고 강제 삭제는 참여자부터 제거한다")
    void delete_and_force_delete() {
        Schedule schedule = identifiedSchedule();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.existsAttendanceStatusByScheduleId(SCHEDULE_ID)).willReturn(true);
        assertError(() -> sut.delete(SCHEDULE_ID), ScheduleErrorCode.SCHEDULE_HAS_ATTENDANCE_RECORD);

        given(loadScheduleParticipantPort.existsAttendanceStatusByScheduleId(SCHEDULE_ID)).willReturn(false);
        sut.delete(SCHEDULE_ID);
        sut.forceDelete(SCHEDULE_ID);

        then(deleteScheduleParticipantPort).should(org.mockito.Mockito.times(2)).deleteByScheduleId(SCHEDULE_ID);
        then(deleteSchedulePort).should(org.mockito.Mockito.times(2)).delete(SCHEDULE_ID);
    }

    @Test
    @DisplayName("삭제와 강제 삭제는 존재하지 않는 일정을 구분한다")
    void delete_not_found() {
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.empty());

        assertError(() -> sut.delete(SCHEDULE_ID), ScheduleErrorCode.SCHEDULE_NOT_FOUND);
        assertError(() -> sut.forceDelete(SCHEDULE_ID), ScheduleErrorCode.SCHEDULE_NOT_FOUND);
    }

    private void prepareCreate(ScheduleCapabilitiesInfo capabilities, int memberCount) {
        given(capabilitiesService.getCapabilities(AUTHOR_ID)).willReturn(capabilities);
        given(getChallengerUseCase.getLatestActiveChallengerByMemberId(AUTHOR_ID))
            .willReturn(challenger(AUTHOR_ID));
        given(getGisuUseCase.getActiveGisu()).willReturn(activeGisu());
        given(getMemberUseCase.countMembersByIds(anySet())).willReturn((long) memberCount);
    }

    private CreateScheduleCommand createCommand(
        Set<Long> participants,
        CreateScheduleCommand.AttendancePolicyInfo attendancePolicy
    ) {
        return CreateScheduleCommand.builder()
            .name("일정")
            .description("설명")
            .tags(Set.of(ScheduleTag.GENERAL))
            .authorMemberId(AUTHOR_ID)
            .startsAt(DEFAULT_STARTS_AT)
            .endsAt(DEFAULT_ENDS_AT)
            .attendancePolicy(attendancePolicy)
            .participantMemberIds(participants)
            .build();
    }

    private Schedule identifiedSchedule() {
        Schedule schedule = schedule();
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);
        return schedule;
    }

    private ChallengerInfoWithStatus challenger(Long memberId) {
        return ChallengerInfoWithStatus.builder().challengerId(1L).memberId(memberId).gisuId(1L).build();
    }

    private GisuInfo activeGisu() {
        return new GisuInfo(
            1L, 1L, DEFAULT_STARTS_AT.minus(1, ChronoUnit.DAYS),
            DEFAULT_ENDS_AT.plus(1, ChronoUnit.DAYS), true
        );
    }

    private void assertError(Runnable action, ScheduleErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ScheduleDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code)
            );
    }
}
