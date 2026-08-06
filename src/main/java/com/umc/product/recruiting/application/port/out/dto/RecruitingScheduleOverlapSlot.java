package com.umc.product.recruiting.application.port.out.dto;

import java.time.Instant;
import java.util.Set;

public record RecruitingScheduleOverlapSlot(
    Instant startsAt,
    Set<Long> availableFormResponseIds
) {

    public RecruitingScheduleOverlapSlot {
        availableFormResponseIds = Set.copyOf(availableFormResponseIds);
    }
}
