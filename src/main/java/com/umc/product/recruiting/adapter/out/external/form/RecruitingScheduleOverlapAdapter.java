package com.umc.product.recruiting.adapter.out.external.form;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.in.query.GetScheduleOverlapUseCase;
import com.umc.product.form.application.port.in.query.dto.ScheduleOverlapSlotInfo;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingScheduleOverlapAdapter implements FindRecruitingScheduleOverlapPort {

    private final GetScheduleOverlapUseCase getScheduleOverlapUseCase;

    @Override
    public List<RecruitingScheduleOverlapSlot> findOverlaps(
        Long formId,
        Long questionId,
        List<Long> formResponseIds,
        Instant rangeStartInclusive,
        Instant rangeEndExclusive
    ) {
        return getScheduleOverlapUseCase.getOverlap(formId, questionId, Set.copyOf(formResponseIds)).stream()
            .filter(slot -> isInRange(slot, rangeStartInclusive, rangeEndExclusive))
            .map(slot -> new RecruitingScheduleOverlapSlot(slot.startsAt(), slot.availableResponseIds()))
            .toList();
    }

    private boolean isInRange(ScheduleOverlapSlotInfo slot, Instant rangeStartInclusive, Instant rangeEndExclusive) {
        return (rangeStartInclusive == null || !slot.startsAt().isBefore(rangeStartInclusive))
            && (rangeEndExclusive == null || slot.startsAt().isBefore(rangeEndExclusive));
    }
}
