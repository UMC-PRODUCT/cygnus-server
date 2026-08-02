package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.application.port.out.dto.RecruitingScheduleOverlapSlot;

public interface FindRecruitingScheduleOverlapPort {

    List<RecruitingScheduleOverlapSlot> findOverlaps(
        Long formId,
        Long questionId,
        List<Long> formResponseIds
    );
}
