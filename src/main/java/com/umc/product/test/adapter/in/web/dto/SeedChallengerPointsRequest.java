package com.umc.product.test.adapter.in.web.dto;

import java.util.List;

import com.umc.product.test.application.port.in.command.dto.SeedChallengerPointsCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SeedChallengerPointsRequest(
    @NotEmpty(message = "challengerIds 는 비어 있을 수 없습니다") List<Long> challengerIds,
    @Positive int countPerChallenger,
    @Size(max = 200, message = "상벌점 설명은 200자 이하여야 합니다") String description
) {

    public SeedChallengerPointsCommand toCommand() {
        return new SeedChallengerPointsCommand(challengerIds, countPerChallenger, description);
    }
}
