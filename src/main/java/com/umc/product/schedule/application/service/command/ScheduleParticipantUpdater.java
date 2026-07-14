package com.umc.product.schedule.application.service.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.schedule.application.port.out.DeleteScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.SaveScheduleParticipantPort;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ScheduleParticipantUpdater {

    private final SaveScheduleParticipantPort saveParticipantPort;
    private final DeleteScheduleParticipantPort deleteParticipantPort;
    private final LoadScheduleParticipantPort loadParticipantPort;
    private final GetMemberUseCase getMemberUseCase;

    Changes update(Schedule schedule, Set<Long> newMemberIds) {
        Set<Long> currentIds = loadParticipantPort.findMemberIdsByScheduleId(schedule.getId());
        if (currentIds.equals(newMemberIds)) {
            return Changes.empty();
        }

        List<ScheduleParticipant> existing = loadParticipantPort.findAllByScheduleId(schedule.getId());
        Set<Long> persistedIds = existing.stream()
            .map(ScheduleParticipant::getMemberId)
            .collect(Collectors.toSet());
        Set<Long> removedIds = difference(persistedIds, newMemberIds);
        Set<Long> addedIds = difference(newMemberIds, persistedIds);

        validateMembers(addedIds);
        deleteRemoved(existing, removedIds);
        saveAdded(schedule, addedIds);
        return new Changes(Set.copyOf(addedIds), Set.copyOf(removedIds));
    }

    private Set<Long> difference(Set<Long> source, Set<Long> excluded) {
        Set<Long> difference = new HashSet<>(source);
        difference.removeAll(excluded);
        return difference;
    }

    private void validateMembers(Set<Long> memberIds) {
        if (!memberIds.isEmpty() && getMemberUseCase.countMembersByIds(memberIds) != memberIds.size()) {
            throw new ScheduleDomainException(ScheduleErrorCode.INVALID_MEMBER_INVITE);
        }
    }

    private void deleteRemoved(List<ScheduleParticipant> existing, Set<Long> removedIds) {
        if (removedIds.isEmpty()) {
            return;
        }
        deleteParticipantPort.deleteAll(existing.stream()
            .filter(participant -> removedIds.contains(participant.getMemberId()))
            .toList());
    }

    private void saveAdded(Schedule schedule, Set<Long> addedIds) {
        if (addedIds.isEmpty()) {
            return;
        }
        saveParticipantPort.saveAll(addedIds.stream()
            .map(memberId -> ScheduleParticipant.builder()
                .memberId(memberId)
                .schedule(schedule)
                .attendance(null)
                .build())
            .toList());
    }

    record Changes(Set<Long> addedMemberIds, Set<Long> removedMemberIds) {

        static Changes empty() {
            return new Changes(Set.of(), Set.of());
        }
    }
}
