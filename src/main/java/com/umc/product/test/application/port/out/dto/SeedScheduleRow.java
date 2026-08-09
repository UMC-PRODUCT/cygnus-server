package com.umc.product.test.application.port.out.dto;

import java.time.Instant;

public record SeedScheduleRow(
    long id,
    String name,
    long authorMemberId,
    Instant startsAt,
    Instant endsAt
) {
}
