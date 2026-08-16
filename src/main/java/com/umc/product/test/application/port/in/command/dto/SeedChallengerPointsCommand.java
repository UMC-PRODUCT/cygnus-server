package com.umc.product.test.application.port.in.command.dto;

import java.util.List;

public record SeedChallengerPointsCommand(
    List<Long> challengerIds,
    int countPerChallenger,
    String description
) {
}
