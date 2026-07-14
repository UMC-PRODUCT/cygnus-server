package com.umc.product.schedule.application.service.command;

import static com.umc.product.schedule.application.service.command.ScheduleCommandAuditFixtures.AUTHOR_MEMBER_ID;
import static com.umc.product.schedule.application.service.command.ScheduleCommandAuditFixtures.ENDS_AT;
import static com.umc.product.schedule.application.service.command.ScheduleCommandAuditFixtures.SCHEDULE_ID;
import static com.umc.product.schedule.application.service.command.ScheduleCommandAuditFixtures.STARTS_AT;
import static com.umc.product.schedule.application.service.command.ScheduleCommandAuditFixtures.assertScheduleSnapshot;
import static com.umc.product.schedule.application.service.command.ScheduleCommandAuditFixtures.command;
import static com.umc.product.schedule.application.service.command.ScheduleCommandAuditFixtures.schedule;
import static com.umc.product.schedule.application.service.command.ScheduleCommandAuditFixtures.section;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfoWithStatus;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
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
import com.umc.product.schedule.domain.enums.ScheduleTag;

@ExtendWith(MockitoExtension.class)
@DisplayName("일정 rich audit")
class ScheduleCommandServiceAuditTest {

    @Mock
    SaveSchedulePort saveSchedulePort;
    @Mock
    LoadSchedulePort loadSchedulePort;
    @Mock
    DeleteSchedulePort deleteSchedulePort;
    @Mock
    SaveScheduleParticipantPort saveScheduleParticipantPort;
    @Mock
    DeleteScheduleParticipantPort deleteScheduleParticipantPort;
    @Mock
    LoadScheduleParticipantPort loadScheduleParticipantPort;
    @Mock
    ScheduleCapabilitiesService capabilitiesService;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;
    @Mock
    ScheduleParticipantUpdater participantUpdater;
    ScheduleAuditRecorder auditRecorder;
    ScheduleCommandService sut;

    @BeforeEach
    void setUpMemberSnapshots() {
        auditRecorder = new ScheduleAuditRecorder(getMemberUseCase, recordAuditLogUseCase);
        sut = new ScheduleCommandService(
            saveSchedulePort,
            loadSchedulePort,
            deleteSchedulePort,
            saveScheduleParticipantPort,
            deleteScheduleParticipantPort,
            loadScheduleParticipantPort,
            capabilitiesService,
            getChallengerUseCase,
            getGisuUseCase,
            getMemberUseCase,
            auditRecorder,
            participantUpdater
        );
        given(getMemberUseCase.findAllByIds(any())).willAnswer(invocation -> {
            Set<Long> ids = invocation.getArgument(0);
            return ids.stream().collect(Collectors.toMap(
                Function.identity(),
                ScheduleCommandAuditFixtures::member
            ));
        });
    }

    @Test
    @DisplayName("일정 생성은 일정과 초대 대상자의 당시 스냅샷을 함께 발행한다")
    void 일정_생성은_일정과_참여자_스냅샷을_발행한다() {
        // given
        Set<Long> participantIds = Set.of(21L, 22L);
        CreateScheduleCommand command = CreateScheduleCommand.builder()
            .name("정기 세션")
            .description("감사 로그에 저장하면 안 되는 긴 설명")
            .tags(Set.of(ScheduleTag.STUDY))
            .authorMemberId(AUTHOR_MEMBER_ID)
            .startsAt(STARTS_AT)
            .endsAt(ENDS_AT)
            .participantMemberIds(participantIds)
            .build();
        Schedule saved = schedule("정기 세션");

        given(capabilitiesService.getCapabilities(AUTHOR_MEMBER_ID))
            .willReturn(ScheduleCapabilitiesInfo.forCentralMember());
        given(getChallengerUseCase.getLatestActiveChallengerByMemberId(AUTHOR_MEMBER_ID))
            .willReturn(ChallengerInfoWithStatus.builder()
                .memberId(AUTHOR_MEMBER_ID)
                .part(ChallengerPart.SPRINGBOOT)
                .status(ChallengerStatus.ACTIVE)
                .build());
        given(getGisuUseCase.getActiveGisu())
            .willReturn(new GisuInfo(1L, 9L, STARTS_AT.minusSeconds(3_600), ENDS_AT.plusSeconds(3_600), true));
        given(getMemberUseCase.countMembersByIds(participantIds)).willReturn(2L);
        given(saveSchedulePort.save(any(Schedule.class))).willReturn(saved);

        // when
        Long result = sut.create(command);

        // then
        assertThat(result).isEqualTo(SCHEDULE_ID);
        List<RecordAuditLogCommand> events = capturedCommands();
        assertThat(events).hasSize(3);

        RecordAuditLogCommand scheduleEvent = command(events, "Schedule", String.valueOf(SCHEDULE_ID));
        assertThat(scheduleEvent.action()).isEqualTo(AuditAction.CREATE);
        assertThat(scheduleEvent.source()).isEqualTo(AuditSource.EXPLICIT_RECORDER);
        assertScheduleSnapshot(section(scheduleEvent, "target"), "정기 세션");
        assertThat(section(scheduleEvent, "actor"))
            .containsEntry("memberId", AUTHOR_MEMBER_ID)
            .containsEntry("name", "일정작성자")
            .containsEntry("nickname", "일정작성자")
            .containsEntry("schoolName", "테스트대학교");
        assertThat(section(scheduleEvent, "target")).containsEntry("participantCount", 2);

        List<RecordAuditLogCommand> participantEvents = events.stream()
            .filter(event -> event.targetType().equals("ScheduleParticipant"))
            .toList();
        assertThat(participantEvents).extracting(RecordAuditLogCommand::targetId)
            .containsExactlyInAnyOrder("100:21", "100:22");
        assertThat(participantEvents)
            .allSatisfy(event -> assertScheduleSnapshot(section(event, "before"), "정기 세션"));
        assertThat(participantEvents).extracting(event -> section(event, "target").get("name"))
            .containsExactlyInAnyOrder("참여자일", "참여자이");
        assertThat(events.toString())
            .doesNotContain("감사 로그에 저장하면 안 되는 긴 설명", "@test.com", "email", "content", "body");
    }

    @Test
    @DisplayName("일정 수정은 변경 전후의 이름과 상태와 기간을 모두 보존한다")
    void 일정_수정은_변경_전후_스냅샷을_발행한다() {
        // given
        Schedule schedule = schedule("수정 전 일정");
        EditScheduleCommand command = EditScheduleCommand.builder()
            .scheduleId(SCHEDULE_ID)
            .actorMemberId(AUTHOR_MEMBER_ID)
            .name("수정 후 일정")
            .startsAt(STARTS_AT.plusSeconds(600))
            .endsAt(ENDS_AT.plusSeconds(600))
            .build();
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(saveSchedulePort.save(schedule)).willReturn(schedule);

        // when
        sut.update(command);

        // then
        RecordAuditLogCommand event = capturedCommands().getFirst();
        assertThat(event.action()).isEqualTo(AuditAction.UPDATE);
        assertThat(section(event, "before"))
            .containsEntry("name", "수정 전 일정")
            .containsEntry("status", "UPCOMING")
            .containsEntry("period", "2030-07-20T10:00:00Z/2030-07-20T12:00:00Z");
        assertThat(section(event, "after"))
            .containsEntry("name", "수정 후 일정")
            .containsEntry("status", "UPCOMING")
            .containsEntry("period", "2030-07-20T10:10:00Z/2030-07-20T12:10:00Z");
    }

    @Test
    @DisplayName("일정 삭제는 실제 삭제 전에 만든 스냅샷을 발행한다")
    void 일정_삭제는_삭제_전_스냅샷을_발행한다() {
        // given
        Schedule schedule = schedule("삭제 전 일정");
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));
        given(loadScheduleParticipantPort.existsAttendanceStatusByScheduleId(SCHEDULE_ID)).willReturn(false);
        org.mockito.Mockito.doAnswer(invocation -> {
            ReflectionTestUtils.setField(schedule, "name", "삭제 후 훼손");
            return null;
        }).when(deleteSchedulePort).delete(SCHEDULE_ID);

        // when
        sut.delete(SCHEDULE_ID, AUTHOR_MEMBER_ID);

        // then
        RecordAuditLogCommand event = capturedCommands().getFirst();
        assertThat(event.action()).isEqualTo(AuditAction.DELETE);
        assertThat(event.description()).isEqualTo("일정을 삭제했습니다.");
        assertScheduleSnapshot(section(event, "target"), "삭제 전 일정");
        assertScheduleSnapshot(section(event, "before"), "삭제 전 일정");
        then(deleteScheduleParticipantPort).should().deleteByScheduleId(SCHEDULE_ID);
    }

    @Test
    @DisplayName("일정 강제 삭제도 일반 삭제와 구분된 삭제 전 스냅샷을 발행한다")
    void 일정_강제_삭제는_삭제_전_스냅샷을_발행한다() {
        // given
        Schedule schedule = schedule("강제 삭제 전 일정");
        given(loadSchedulePort.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

        // when
        sut.forceDelete(SCHEDULE_ID, AUTHOR_MEMBER_ID);

        // then
        RecordAuditLogCommand event = capturedCommands().getFirst();
        assertThat(event.action()).isEqualTo(AuditAction.DELETE);
        assertThat(event.description()).isEqualTo("일정을 강제로 삭제했습니다.");
        assertScheduleSnapshot(section(event, "target"), "강제 삭제 전 일정");
        assertScheduleSnapshot(section(event, "before"), "강제 삭제 전 일정");
    }

    private List<RecordAuditLogCommand> capturedCommands() {
        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(RecordAuditLogCommand.class);
        then(recordAuditLogUseCase).should(atLeastOnce()).record(captor.capture());
        return captor.getAllValues();
    }

}
