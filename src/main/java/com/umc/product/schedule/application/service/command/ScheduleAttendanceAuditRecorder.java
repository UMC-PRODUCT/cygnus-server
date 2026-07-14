package com.umc.product.schedule.application.service.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.enums.AttendanceStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class ScheduleAttendanceAuditRecorder {

    private final GetMemberUseCase getMemberUseCase;
    private final RecordAuditLogUseCase recordAuditLogUseCase;

    void recordSelf(Schedule schedule, Long memberId, AttendanceStatus status, AuditAction action) {
        MemberInfo member = loadOptional(Set.of(memberId)).get(memberId);
        record(List.of(ScheduleAuditEventFactory.attendanceChanged(
            ScheduleAuditEventFactory.snapshot(schedule),
            memberId,
            member,
            memberId,
            member,
            status,
            action
        )));
    }

    Map<Long, MemberInfo> recordDecisions(List<Decision> decisions) {
        Set<Long> memberIds = new HashSet<>();
        for (Decision decision : decisions) {
            memberIds.add(decision.actorMemberId());
            memberIds.add(decision.participantMemberId());
        }
        Map<Long, MemberInfo> members = getMemberUseCase.batchGetByIds(memberIds);
        List<RecordAuditLogCommand> commands = new ArrayList<>(decisions.size());
        for (Decision decision : decisions) {
            commands.add(ScheduleAuditEventFactory.attendanceChanged(
                decision.schedule(),
                decision.actorMemberId(),
                members.get(decision.actorMemberId()),
                decision.participantMemberId(),
                members.get(decision.participantMemberId()),
                decision.status(),
                decision.action()
            ));
        }
        record(commands);
        return members;
    }

    private Map<Long, MemberInfo> loadOptional(Set<Long> memberIds) {
        try {
            Map<Long, MemberInfo> members = getMemberUseCase.findAllByIds(memberIds);
            return members == null ? Map.of() : members;
        } catch (RuntimeException exception) {
            log.warn("일정 출석 감사 회원 스냅샷 조회 실패: count={}, errorType={}",
                memberIds.size(), exception.getClass().getSimpleName(), exception);
            return Map.of();
        }
    }

    private void record(List<RecordAuditLogCommand> commands) {
        for (RecordAuditLogCommand command : commands) {
            try {
                recordAuditLogUseCase.record(command);
            } catch (RuntimeException exception) {
                log.warn("일정 출석 rich audit 기록 호출 실패: action={}, errorType={}",
                    command.action(), exception.getClass().getSimpleName());
            }
        }
    }

    record Decision(
        ScheduleAuditEventFactory.ScheduleSnapshot schedule,
        Long actorMemberId,
        Long participantMemberId,
        AttendanceStatus status,
        AuditAction action
    ) {

        static Decision of(
            ScheduleAuditEventFactory.ScheduleSnapshot schedule,
            Long actorMemberId,
            Long participantMemberId,
            AttendanceStatus status,
            AuditAction action
        ) {
            return new Decision(schedule, actorMemberId, participantMemberId, status, action);
        }
    }
}
