package com.umc.product.schedule.adapter.in.web.v2;

import static com.umc.product.support.fixture.ScheduleUnitFixture.DEFAULT_ENDS_AT;
import static com.umc.product.support.fixture.ScheduleUnitFixture.DEFAULT_STARTS_AT;
import static com.umc.product.support.fixture.ScheduleUnitFixture.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.CreateScheduleRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.DecideAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.EditScheduleRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.ExcuseScheduleAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.request.ScheduleAttendanceRequest;
import com.umc.product.schedule.adapter.in.web.v2.dto.response.AdminScheduleInfoResponse;
import com.umc.product.schedule.adapter.in.web.v2.dto.response.ScheduleInfoResponse;
import com.umc.product.schedule.adapter.in.web.v2.dto.response.ScheduleParticipantAttendanceInfoResponse;
import com.umc.product.schedule.application.port.in.command.CreateScheduleParticipantUseCase;
import com.umc.product.schedule.application.port.in.command.CreateScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.DeleteScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.UpdateScheduleParticipantUseCase;
import com.umc.product.schedule.application.port.in.command.UpdateScheduleUseCase;
import com.umc.product.schedule.application.port.in.command.dto.CreateScheduleCommand;
import com.umc.product.schedule.application.port.in.command.dto.DecideAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.EditScheduleCommand;
import com.umc.product.schedule.application.port.in.command.dto.ExcuseScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.ScheduleAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.result.ScheduleParticipantAttendanceResult;
import com.umc.product.schedule.application.port.in.query.GetScheduleCapabilitiesUseCase;
import com.umc.product.schedule.application.port.in.query.GetScheduleUseCase;
import com.umc.product.schedule.application.port.in.query.dto.AdminScheduleInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleCapabilitiesInfo;
import com.umc.product.schedule.application.port.in.query.dto.ScheduleInfo;
import com.umc.product.schedule.application.port.out.dto.ScheduleParticipantDetailDto;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;

@ExtendWith(MockitoExtension.class)
@DisplayName("Schedule v2 controller")
class ScheduleControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    @Mock CreateScheduleUseCase createScheduleUseCase;
    @Mock UpdateScheduleUseCase updateScheduleUseCase;
    @Mock DeleteScheduleUseCase deleteScheduleUseCase;
    @Mock CreateScheduleParticipantUseCase createScheduleParticipantUseCase;
    @Mock UpdateScheduleParticipantUseCase updateScheduleParticipantUseCase;
    @Mock GetScheduleUseCase getScheduleUseCase;
    @Mock GetScheduleCapabilitiesUseCase getScheduleCapabilitiesUseCase;

    ScheduleCommandV2Controller commandController;
    ScheduleQueryV2Controller queryController;
    MemberPrincipal principal;

    @BeforeEach
    void setUp() {
        commandController = new ScheduleCommandV2Controller(
            createScheduleUseCase, updateScheduleUseCase, deleteScheduleUseCase,
            createScheduleParticipantUseCase, updateScheduleParticipantUseCase
        );
        queryController = new ScheduleQueryV2Controller(getScheduleUseCase, getScheduleCapabilitiesUseCase);
        principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
    }

    @Test
    @DisplayName("생성·수정 request에 현재 회원과 경로 ID를 결합해 usecase에 전달한다")
    void create_and_edit_delegate_commands() {
        given(createScheduleUseCase.create(any(CreateScheduleCommand.class))).willReturn(SCHEDULE_ID);
        given(updateScheduleUseCase.update(any(EditScheduleCommand.class))).willReturn(SCHEDULE_ID);
        CreateScheduleRequest createRequest = new CreateScheduleRequest(
            "일정", "설명", Set.of(ScheduleTag.GENERAL), DEFAULT_STARTS_AT, DEFAULT_ENDS_AT,
            null, null, Set.of(2L)
        );
        EditScheduleRequest editRequest = new EditScheduleRequest(
            "수정", null, null, null, null, null, null, null, null, null
        );

        assertThat(commandController.create(createRequest, principal)).isEqualTo(SCHEDULE_ID);
        assertThat(commandController.edit(SCHEDULE_ID, editRequest, principal)).isEqualTo(SCHEDULE_ID);

        then(createScheduleUseCase).should().create(org.mockito.ArgumentMatchers.argThat(command ->
            command.authorMemberId().equals(MEMBER_ID) && command.participantMemberIds().equals(Set.of(2L))
        ));
        then(updateScheduleUseCase).should().update(org.mockito.ArgumentMatchers.argThat(command ->
            command.scheduleId().equals(SCHEDULE_ID) && command.name().equals("수정")
        ));
    }

    @Test
    @DisplayName("일반 삭제와 강제 삭제를 구분해 위임한다")
    void delete_and_force_delete_delegate() {
        commandController.delete(SCHEDULE_ID);
        commandController.forceDelete(SCHEDULE_ID);

        then(deleteScheduleUseCase).should().delete(SCHEDULE_ID);
        then(deleteScheduleUseCase).should().forceDelete(SCHEDULE_ID);
    }

    @Test
    @DisplayName("출석·사유 요청은 현재 회원 command와 결과 response를 변환한다")
    void attendance_and_excuse_mapping() {
        ScheduleParticipantAttendanceResult attendanceResult = result(AttendanceStatus.PRESENT_PENDING);
        ScheduleParticipantAttendanceResult excuseResult = result(AttendanceStatus.EXCUSED_PENDING);
        given(createScheduleParticipantUseCase.createScheduleParticipantWithAttendance(
            any(ScheduleAttendanceCommand.class))).willReturn(attendanceResult);
        given(createScheduleParticipantUseCase.createExcusedScheduleParticipantWithAttendance(
            any(ExcuseScheduleAttendanceCommand.class))).willReturn(excuseResult);

        ScheduleParticipantAttendanceInfoResponse attendance = commandController.requestAttendance(
            principal, SCHEDULE_ID, new ScheduleAttendanceRequest(true, 37.0, 127.0)
        );
        ScheduleParticipantAttendanceInfoResponse excuse = commandController.excuseAttendance(
            principal, SCHEDULE_ID, new ExcuseScheduleAttendanceRequest(false, null, null, "병원")
        );

        assertThat(attendance.status()).isEqualTo(AttendanceStatus.PRESENT_PENDING);
        assertThat(excuse.status()).isEqualTo(AttendanceStatus.EXCUSED_PENDING);
        then(createScheduleParticipantUseCase).should().createScheduleParticipantWithAttendance(
            org.mockito.ArgumentMatchers.argThat(command ->
                command.scheduleId().equals(SCHEDULE_ID) && command.requesterMemberId().equals(MEMBER_ID)
            ));
    }

    @Test
    @DisplayName("출석 결정 목록은 입력 순서대로 command와 response를 변환한다")
    void decisions_preserve_order() {
        given(updateScheduleParticipantUseCase.decideAttendances(any()))
            .willReturn(List.of(result(AttendanceStatus.PRESENT), result(AttendanceStatus.ABSENT)));

        List<ScheduleParticipantAttendanceInfoResponse> response = commandController.decideAttendances(
            principal,
            SCHEDULE_ID,
            List.of(new DecideAttendanceRequest(2L, true, "승인"),
                new DecideAttendanceRequest(3L, false, "반려"))
        );

        assertThat(response).extracting(ScheduleParticipantAttendanceInfoResponse::status)
            .containsExactly(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT);
        ArgumentCaptor<List<DecideAttendanceCommand>> captor = ArgumentCaptor.forClass(List.class);
        then(updateScheduleParticipantUseCase).should().decideAttendances(captor.capture());
        assertThat(captor.getValue()).extracting(DecideAttendanceCommand::participantMemberId)
            .containsExactly(2L, 3L);
        assertThat(captor.getValue()).allSatisfy(command ->
            assertThat(command.decidedByMemberId()).isEqualTo(MEMBER_ID));
    }

    @Test
    @DisplayName("capability·내 일정·상세 query 결과를 response로 변환한다")
    void basic_queries_map_responses() {
        ScheduleInfo info = info();
        given(getScheduleCapabilitiesUseCase.getCapabilities(MEMBER_ID))
            .willReturn(ScheduleCapabilitiesInfo.forSchoolAdmin());
        given(getScheduleUseCase.searchMySchedules(DEFAULT_STARTS_AT, DEFAULT_ENDS_AT, true, MEMBER_ID))
            .willReturn(List.of(info));
        given(getScheduleUseCase.getScheduleDetails(SCHEDULE_ID, MEMBER_ID)).willReturn(info);

        assertThat(queryController.getCapabilities(principal).maxParticipantCount()).isEqualTo(100);
        List<ScheduleInfoResponse> mine = queryController.mySchedules(
            DEFAULT_STARTS_AT, DEFAULT_ENDS_AT, true, principal
        );
        assertThat(mine).singleElement().extracting(ScheduleInfoResponse::scheduleId).isEqualTo(SCHEDULE_ID);
        assertThat(queryController.details(SCHEDULE_ID, principal).scheduleId()).isEqualTo(SCHEDULE_ID);
    }

    @Test
    @DisplayName("운영진 목록은 제공된 기간을 유지하고 결과를 response로 변환한다")
    void admin_list_uses_explicit_period() {
        AdminScheduleInfo info = adminInfo();
        given(getScheduleUseCase.searchAdminSchedules(
            DEFAULT_STARTS_AT, DEFAULT_ENDS_AT, AttendanceStatus.PRESENT, MEMBER_ID
        )).willReturn(List.of(info));

        List<AdminScheduleInfoResponse> response = queryController.getAttendanceInfoList(
            DEFAULT_STARTS_AT, DEFAULT_ENDS_AT, AttendanceStatus.PRESENT, principal
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).participants()).hasSize(1);
    }

    @Test
    @DisplayName("운영진 목록의 생략 기간은 현재 기준 30일 전부터 24시간 후까지 계산한다")
    void admin_list_applies_default_period() {
        Instant before = Instant.now();
        given(getScheduleUseCase.searchAdminSchedules(any(), any(),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(MEMBER_ID)))
            .willReturn(List.of());

        assertThat(queryController.getAttendanceInfoList(null, null, null, principal)).isEmpty();

        Instant after = Instant.now();
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        then(getScheduleUseCase).should().searchAdminSchedules(
            from.capture(), to.capture(), org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq(MEMBER_ID)
        );
        assertThat(from.getValue()).isBetween(before.minusSeconds(30L * 24 * 3600),
            after.minusSeconds(30L * 24 * 3600));
        assertThat(to.getValue()).isBetween(before.plusSeconds(24L * 3600), after.plusSeconds(24L * 3600));
    }

    @Test
    @DisplayName("운영진 단건 출석 조회를 현재 회원·상태와 결합한다")
    void admin_detail_maps_response() {
        given(getScheduleUseCase.getAdminSchedule(SCHEDULE_ID, MEMBER_ID, AttendanceStatus.ABSENT))
            .willReturn(adminInfo());

        AdminScheduleInfoResponse response = queryController.getAttendanceInfo(
            AttendanceStatus.ABSENT, principal, SCHEDULE_ID
        );

        assertThat(response.participants()).singleElement()
            .extracting(AdminScheduleInfoResponse.AdminScheduleParticipantInfoResponse::attendanceStatus)
            .isEqualTo(AttendanceStatus.PRESENT);
    }

    private ScheduleParticipantAttendanceResult result(AttendanceStatus status) {
        return ScheduleParticipantAttendanceResult.builder()
            .latitude(37.0).longitude(127.0).status(status)
            .isPendingDecision(status.isPending()).hasDecisionMakerMember(false).build();
    }

    private ScheduleInfo info() {
        Schedule schedule = identifiedSchedule();
        return ScheduleInfo.from(schedule, List.of(detail()), MEMBER_ID);
    }

    private AdminScheduleInfo adminInfo() {
        return AdminScheduleInfo.from(identifiedSchedule(), List.of(detail()));
    }

    private Schedule identifiedSchedule() {
        Schedule schedule = schedule();
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);
        return schedule;
    }

    private ScheduleParticipantDetailDto detail() {
        return new ScheduleParticipantDetailDto(
            SCHEDULE_ID, MEMBER_ID, "회원", "닉네임", 1L, "학교", null,
            AttendanceStatus.PRESENT, null, true
        );
    }
}
