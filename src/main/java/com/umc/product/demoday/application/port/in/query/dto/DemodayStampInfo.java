package com.umc.product.demoday.application.port.in.query.dto;

import java.time.Instant;

public record DemodayStampInfo(
        Long boothId,
        Instant collectedAt
) {
}
