package com.umc.product.recruiting.application.port.out;

import java.time.Instant;
import java.util.List;

import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;

public interface FindRecruitingScheduleOverlapPort {

    /**
     * @param rangeStartInclusive null이면 시작 범위를 제한하지 않음
     * @param rangeEndExclusive null이면 종료 범위를 제한하지 않음
     */
    List<RecruitingScheduleOverlapSlot> findOverlaps(
        Long formId,
        Long questionId,
        List<Long> formResponseIds,
        Instant rangeStartInclusive,
        Instant rangeEndExclusive
    );
}
