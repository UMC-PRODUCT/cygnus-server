package com.umc.product.test.adapter.in.web.dto;

import com.umc.product.test.application.port.in.command.dto.SeedChallengerPointsResult;

public record SeedChallengerPointsResponse(
    int challengerCount,
    int grantedPointCount
) {
    public static SeedChallengerPointsResponse from(SeedChallengerPointsResult result) {
        return new SeedChallengerPointsResponse(result.challengerCount(), result.grantedPointCount());
    }
}
