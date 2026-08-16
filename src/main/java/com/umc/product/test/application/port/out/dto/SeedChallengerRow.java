package com.umc.product.test.application.port.out.dto;

import com.umc.product.common.domain.enums.ChallengerPart;

public record SeedChallengerRow(
    long id,
    long memberId,
    ChallengerPart part,
    long gisuId
) {
}
