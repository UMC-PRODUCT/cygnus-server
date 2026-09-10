package com.umc.product.test.application.port.out.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record SeedChallengerRow(
    long id,
    long memberId,
    ChallengerPart part,
    List<ChallengerTrack> tracks,
    long gisuId
) {
}
