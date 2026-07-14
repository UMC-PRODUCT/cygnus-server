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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class ScheduleAuditRecorder {

    private final GetMemberUseCase getMemberUseCase;
    private final RecordAuditLogUseCase recordAuditLogUseCase;

    ScheduleAuditEventFactory.ScheduleSnapshot capture(Schedule schedule) {
        return ScheduleAuditEventFactory.snapshot(schedule);
    }

    void recordCreated(Schedule schedule, Set<Long> participantMemberIds) {
        Set<Long> memberIds = new HashSet<>(participantMemberIds);
        memberIds.add(schedule.getAuthorMemberId());
        Map<Long, MemberInfo> members = loadMemberSnapshots(memberIds);

        List<RecordAuditLogCommand> commands = new ArrayList<>();
        commands.add(ScheduleAuditEventFactory.scheduleCreated(
            schedule,
            members.get(schedule.getAuthorMemberId()),
            participantMemberIds.size()
        ));
        appendParticipantCommands(
            commands,
            ScheduleAuditEventFactory.snapshot(schedule, participantMemberIds.size()),
            schedule.getAuthorMemberId(),
            members,
            participantMemberIds,
            Set.of()
        );
        recordAll(commands);
    }

    void recordUpdated(
        ScheduleAuditEventFactory.ScheduleSnapshot before,
        Schedule schedule,
        Long actorMemberId,
        Set<Long> addedMemberIds,
        Set<Long> removedMemberIds
    ) {
        Set<Long> memberIds = new HashSet<>(addedMemberIds);
        memberIds.addAll(removedMemberIds);
        addIfPresent(memberIds, actorMemberId);
        Map<Long, MemberInfo> members = loadMemberSnapshots(memberIds);

        List<RecordAuditLogCommand> commands = new ArrayList<>();
        commands.add(ScheduleAuditEventFactory.scheduleUpdated(
            before,
            schedule,
            actorMemberId,
            members.get(actorMemberId)
        ));
        appendParticipantCommands(
            commands,
            ScheduleAuditEventFactory.snapshot(schedule),
            actorMemberId,
            members,
            addedMemberIds,
            removedMemberIds
        );
        recordAll(commands);
    }

    void recordDeleted(
        ScheduleAuditEventFactory.ScheduleSnapshot before,
        Long actorMemberId,
        boolean forced
    ) {
        Set<Long> actorIds = new HashSet<>();
        addIfPresent(actorIds, actorMemberId);
        MemberInfo actor = actorMemberId == null
            ? null
            : loadMemberSnapshots(actorIds).get(actorMemberId);
        recordAll(List.of(ScheduleAuditEventFactory.scheduleDeleted(
            before,
            actorMemberId,
            actor,
            forced
        )));
    }

    private void appendParticipantCommands(
        List<RecordAuditLogCommand> commands,
        ScheduleAuditEventFactory.ScheduleSnapshot schedule,
        Long actorMemberId,
        Map<Long, MemberInfo> members,
        Set<Long> addedMemberIds,
        Set<Long> removedMemberIds
    ) {
        addedMemberIds.stream().sorted().map(memberId ->
            ScheduleAuditEventFactory.participantChanged(
                schedule,
                actorMemberId,
                members.get(actorMemberId),
                memberId,
                members.get(memberId),
                AuditAction.CREATE
            )).forEach(commands::add);
        removedMemberIds.stream().sorted().map(memberId ->
            ScheduleAuditEventFactory.participantChanged(
                schedule,
                actorMemberId,
                members.get(actorMemberId),
                memberId,
                members.get(memberId),
                AuditAction.DELETE
            )).forEach(commands::add);
    }

    private Map<Long, MemberInfo> loadMemberSnapshots(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        try {
            Map<Long, MemberInfo> members = getMemberUseCase.findAllByIds(memberIds);
            return members == null ? Map.of() : members;
        } catch (RuntimeException exception) {
            log.warn(
                "일정 감사 회원 snapshot 조회 실패: count={}, errorType={}",
                memberIds.size(),
                exception.getClass().getSimpleName(),
                exception
            );
            return Map.of();
        }
    }

    private void addIfPresent(Set<Long> memberIds, Long memberId) {
        if (memberId != null) {
            memberIds.add(memberId);
        }
    }

    private void recordAll(List<RecordAuditLogCommand> commands) {
        for (RecordAuditLogCommand command : commands) {
            try {
                recordAuditLogUseCase.record(command);
            } catch (RuntimeException exception) {
                log.warn("일정 rich audit 기록 호출 실패: action={}, errorType={}",
                    command.action(), exception.getClass().getSimpleName());
            }
        }
    }
}
