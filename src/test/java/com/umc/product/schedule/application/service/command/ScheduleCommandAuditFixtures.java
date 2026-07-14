package com.umc.product.schedule.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.enums.ScheduleTag;

final class ScheduleCommandAuditFixtures {

    static final long AUTHOR_MEMBER_ID = 10L;
    static final long SCHEDULE_ID = 100L;
    static final Instant STARTS_AT = Instant.parse("2030-07-20T10:00:00Z");
    static final Instant ENDS_AT = Instant.parse("2030-07-20T12:00:00Z");

    private ScheduleCommandAuditFixtures() {
    }

    static RecordAuditLogCommand command(
        List<RecordAuditLogCommand> commands,
        String targetType,
        String targetId
    ) {
        return commands.stream()
            .filter(event -> event.targetType().equals(targetType) && event.targetId().equals(targetId))
            .findFirst()
            .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> section(RecordAuditLogCommand command, String name) {
        return (Map<String, Object>) command.details().get(name);
    }

    static void assertScheduleSnapshot(Map<String, Object> snapshot, String name) {
        assertThat(snapshot)
            .containsEntry("type", "Schedule")
            .containsEntry("id", SCHEDULE_ID)
            .containsEntry("name", name)
            .containsEntry("status", "UPCOMING")
            .containsEntry("period", STARTS_AT + "/" + ENDS_AT);
    }

    static Schedule schedule(String name) {
        Schedule schedule = Schedule.builder()
            .name(name)
            .description("일정 상세 설명")
            .tags(Set.of(ScheduleTag.STUDY))
            .authorMemberId(AUTHOR_MEMBER_ID)
            .startsAt(STARTS_AT)
            .endsAt(ENDS_AT)
            .build();
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);
        return schedule;
    }

    static MemberInfo member(Long id) {
        String name = switch (id.intValue()) {
            case 10 -> "일정작성자";
            case 21 -> "참여자일";
            case 22 -> "참여자이";
            default -> "회원" + id;
        };
        return MemberInfo.builder()
            .id(id)
            .name(name)
            .nickname(name)
            .email(name + "@test.com")
            .schoolName("테스트대학교")
            .status(MemberStatus.ACTIVE)
            .build();
    }
}
