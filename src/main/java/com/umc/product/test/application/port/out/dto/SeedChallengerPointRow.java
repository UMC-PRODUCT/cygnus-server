package com.umc.product.test.application.port.out.dto;

import com.umc.product.challenger.domain.enums.PointType;

public record SeedChallengerPointRow(
    long challengerId,
    PointType pointType,
    String description
) {
}
