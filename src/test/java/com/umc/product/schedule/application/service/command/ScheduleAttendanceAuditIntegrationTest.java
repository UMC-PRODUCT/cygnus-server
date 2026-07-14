package com.umc.product.schedule.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.domain.Member;
import com.umc.product.schedule.application.port.in.command.dto.CreateScheduleCommand;
import com.umc.product.schedule.application.port.in.command.dto.DecideAttendanceCommand;
import com.umc.product.schedule.application.port.in.command.dto.EditScheduleCommand;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;

@DisplayName("일정 출석 rich audit 실제 UseCase-PostgreSQL 통합 계약")
class ScheduleAttendanceAuditIntegrationTest extends ScheduleAuditIntegrationSupport {

    @Test
    @DisplayName("실제 일정 생성 UseCase는 일정과 참여자 snapshot을 PostgreSQL JSON 행으로 저장한다")
    void 일정_생성은_일정과_참여자_snapshot을_감사_행에_저장한다() throws Exception {
        // given
        Member author = memberFixture.일반("생성감사작성자");
        Member participant = memberFixture.일반("생성감사참여자");
        var gisu = gisuFixture.활성_기수();
        challengerFixture.챌린저(author.getId(), ChallengerPart.SPRINGBOOT, gisu.getId());
        Instant startsAt = Instant.parse("2024-07-20T10:00:00Z");
        Instant endsAt = Instant.parse("2024-07-20T12:00:00Z");

        // when
        Long scheduleId = createScheduleUseCase.create(CreateScheduleCommand.builder()
            .name("실제 생성 일정")
            .description(SENSITIVE_SCHEDULE_TEXT)
            .tags(Set.of(ScheduleTag.STUDY))
            .authorMemberId(author.getId())
            .startsAt(startsAt)
            .endsAt(endsAt)
            .participantMemberIds(Set.of(participant.getId()))
            .build());

        // then
        awaitExplicitRowCount("Schedule", 1L);
        awaitExplicitRowCount("ScheduleParticipant", 1L);
        assertThat(loadSchedulePort.findById(scheduleId)).isPresent();

        Map<String, Object> scheduleRow = explicitRow("Schedule", scheduleId.toString(), "CREATE");
        Map<String, Object> participantRow = explicitRow(
            "ScheduleParticipant", scheduleId + ":" + participant.getId(), "CREATE");
        JsonNode scheduleDetails = details(scheduleRow);
        JsonNode participantDetails = details(participantRow);

        assertThat(scheduleDetails.path("target").path("name").asText()).isEqualTo("실제 생성 일정");
        assertThat(scheduleDetails.path("target").path("status").asText()).isEqualTo("COMPLETED");
        assertThat(scheduleDetails.path("target").path("period").asText())
            .isEqualTo(startsAt + "/" + endsAt);
        assertThat(scheduleDetails.path("after")).isEqualTo(scheduleDetails.path("target"));
        assertThat(participantDetails.path("target").path("memberId").asLong())
            .isEqualTo(participant.getId());
        assertThat(participantDetails.path("target").path("name").asText())
            .isEqualTo(participant.getName());
        assertThat(participantDetails.path("before").path("id").asLong()).isEqualTo(scheduleId);
        assertThat(participantDetails.path("before").path("name").asText()).isEqualTo("실제 생성 일정");
        assertThat(participantDetails.path("after").path("status").asText()).isEqualTo("INVITED");
        assertSensitiveTextAbsent(scheduleDetails + participantDetails.toString());

    }

    @Test
    @DisplayName("일정 수정과 일반 강제 삭제의 전후 snapshot이 PostgreSQL에 남는다")
    void 수정되고_삭제된_일정의_snapshot은_감사_행에_남는다() throws Exception {
        // given
        Member author = memberFixture.일반("삭제감사작성자");
        Schedule regularBefore = saveSchedule("수정 전 일반 삭제 일정", author.getId(), false);
        Schedule forced = saveSchedule("강제 삭제 일정", author.getId(), false);

        // when
        updateScheduleUseCase.update(EditScheduleCommand.builder()
            .scheduleId(regularBefore.getId())
            .actorMemberId(author.getId())
            .name("수정 후 일반 삭제 일정")
            .build());
        awaitExplicitRowCount("Schedule", 1L);
        Schedule regular = loadSchedulePort.findById(regularBefore.getId()).orElseThrow();
        deleteScheduleUseCase.delete(regular.getId(), author.getId());
        deleteScheduleUseCase.forceDelete(forced.getId(), author.getId());

        // then
        awaitExplicitRowCount("Schedule", 3L);
        assertThat(loadSchedulePort.findById(regular.getId())).isEmpty();
        assertThat(loadSchedulePort.findById(forced.getId())).isEmpty();

        Map<String, Object> updateRow = explicitRow("Schedule", regular.getId().toString(), "UPDATE");
        Map<String, Object> regularRow = explicitRow("Schedule", regular.getId().toString(), "DELETE");
        Map<String, Object> forcedRow = explicitRow("Schedule", forced.getId().toString(), "DELETE");
        JsonNode updateDetails = details(updateRow);
        JsonNode regularDetails = details(regularRow);
        JsonNode forcedDetails = details(forcedRow);

        assertThat(updateDetails.path("before").path("name").asText())
            .isEqualTo("수정 전 일반 삭제 일정");
        assertThat(updateDetails.path("after").path("name").asText())
            .isEqualTo("수정 후 일반 삭제 일정");
        assertThat(updateDetails.path("before").path("status").asText()).isEqualTo("UPCOMING");
        assertThat(updateDetails.path("after").path("status").asText()).isEqualTo("UPCOMING");
        assertThat(updateDetails.path("before").path("period").asText())
            .isEqualTo(updateDetails.path("after").path("period").asText());
        assertDeletedScheduleSnapshot(regularDetails, regular, "수정 후 일반 삭제 일정");
        assertDeletedScheduleSnapshot(forcedDetails, forced, "강제 삭제 일정");
        assertThat(regularRow.get("description")).isEqualTo("일정을 삭제했습니다.");
        assertThat(forcedRow.get("description")).isEqualTo("일정을 강제로 삭제했습니다.");
        assertSensitiveTextAbsent(updateDetails + regularDetails.toString() + forcedDetails);

    }

    @Test
    @DisplayName("출석 공결 및 bulk 승인 반려는 회원과 일정 snapshot을 대상별 JSON 행으로 저장한다")
    void 출석과_bulk_결정은_대상별_감사_JSON을_저장한다() throws Exception {
        // given
        Member first = memberFixture.일반("출석감사대상");
        Member second = memberFixture.일반("공결감사대상");
        Member decider = memberFixture.일반("출석감사결정자");
        Schedule schedule = saveSchedule("실제 출석 일정", decider.getId(), true);
        saveParticipant(schedule, first.getId());
        saveParticipant(schedule, second.getId());

        // when
        createScheduleParticipantUseCase.createScheduleParticipantWithAttendance(
            attendanceCommand(schedule.getId(), first.getId())
        );
        createScheduleParticipantUseCase.createExcusedScheduleParticipantWithAttendance(
            excuseCommand(schedule.getId(), second.getId())
        );
        awaitExplicitRowCount("ScheduleAttendance", 2L);

        updateScheduleParticipantUseCase.decideAttendances(List.of(
            decision(schedule.getId(), decider.getId(), first.getId(), true),
            decision(schedule.getId(), decider.getId(), second.getId(), false)
        ));

        // then
        awaitExplicitRowCount("ScheduleAttendance", 4L);
        Map<String, Object> checkRow = explicitRow(
            "ScheduleAttendance", schedule.getId() + ":" + first.getId(), "CHECK");
        Map<String, Object> submitRow = explicitRow(
            "ScheduleAttendance", schedule.getId() + ":" + second.getId(), "SUBMIT");
        Map<String, Object> approveRow = explicitRow(
            "ScheduleAttendance", schedule.getId() + ":" + first.getId(), "APPROVE");
        Map<String, Object> rejectRow = explicitRow(
            "ScheduleAttendance", schedule.getId() + ":" + second.getId(), "REJECT");

        assertAttendanceSnapshot(details(checkRow), schedule, first, "PRESENT_PENDING");
        assertAttendanceSnapshot(details(submitRow), schedule, second, "EXCUSED_PENDING");
        assertAttendanceSnapshot(details(approveRow), schedule, first, "PRESENT");
        assertAttendanceSnapshot(details(rejectRow), schedule, second, "ABSENT");
        assertThat(approveRow.get("actor_member_id")).isEqualTo(decider.getId());
        assertThat(rejectRow.get("actor_member_id")).isEqualTo(decider.getId());

        String persistedJson = explicitRows("ScheduleAttendance").toString();
        assertSensitiveTextAbsent(persistedJson);
        assertThat(persistedJson).doesNotContain(SENSITIVE_REASON, "latitude", "longitude", "email", "@test.com");
        assertThat(currentStatus(schedule.getId(), first.getId())).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(currentStatus(schedule.getId(), second.getId())).isEqualTo(AttendanceStatus.ABSENT);

    }

    @Test
    @DisplayName("bulk 중간 실패는 첫 대상 변경과 모든 대상별 감사 행을 롤백한다")
    void bulk_중간_실패는_변경과_감사를_모두_롤백한다() {
        // given
        Member participant = memberFixture.일반("부분실패대상");
        Member decider = memberFixture.일반("부분실패결정자");
        Schedule schedule = saveSchedule("부분 실패 일정", decider.getId(), true);
        saveParticipant(schedule, participant.getId());
        createScheduleParticipantUseCase.createScheduleParticipantWithAttendance(
            attendanceCommand(schedule.getId(), participant.getId())
        );
        awaitExplicitRowCount("ScheduleAttendance", 1L);

        // when
        List<DecideAttendanceCommand> commands = List.of(
            decision(schedule.getId(), decider.getId(), participant.getId(), true),
            decision(schedule.getId(), decider.getId(), Long.MAX_VALUE, false)
        );

        // then
        assertThatThrownBy(() -> updateScheduleParticipantUseCase.decideAttendances(commands))
            .isInstanceOf(ScheduleDomainException.class);
        relayAuditEvents();
        assertThat(explicitDecisionRowCount()).isZero();
        assertThat(currentStatus(schedule.getId(), participant.getId()))
            .isEqualTo(AttendanceStatus.PRESENT_PENDING);

    }

    @Test
    @DisplayName("빈 bulk는 빈 결과를 반환하고 101건 bulk도 기존 처리 흐름에 진입한다")
    void 빈_bulk와_초과_bulk는_기존_호환_동작을_유지한다() {
        // given
        List<DecideAttendanceCommand> oversized = LongStream.rangeClosed(1, 101)
            .mapToObj(memberId -> decision(1L, 2L, memberId, true))
            .toList();

        // when & then
        assertThat(updateScheduleParticipantUseCase.decideAttendances(List.of())).isEmpty();
        assertScheduleCode(
            () -> updateScheduleParticipantUseCase.decideAttendances(oversized),
            "SCHEDULE-0009"
        );
        relayAuditEvents();
        assertThat(explicitRows("ScheduleAttendance")).isEmpty();

    }

}
