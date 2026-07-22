package com.umc.product.test.application.port.out.dto;

import java.time.Instant;

public record SeedNoticeRow(
    long id,
    String title,
    String content,
    long authorMemberId,
    Instant createdAt
) {
}
