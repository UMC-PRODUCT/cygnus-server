package com.umc.product.test.application.port.in.command.dto;

public record SeedChallengerPointsResult(
    int challengerCount,
    int grantedPointCount
) {
    public static SeedChallengerPointsResult of(int challengerCount, int grantedPointCount) {
        return new SeedChallengerPointsResult(challengerCount, grantedPointCount);
    }
}
