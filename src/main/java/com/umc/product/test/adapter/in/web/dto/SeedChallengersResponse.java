package com.umc.product.test.adapter.in.web.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.test.application.port.in.command.dto.SeedChallengersResult;

public record SeedChallengersResponse(
    Long gisuId,
    int totalCreated,
    int totalFailed,
    List<PerCellSummary> perCellSummary
) {

    public record PerCellSummary(
        Long chapterId,
        Long schoolId,
        ChallengerPart part,
        int created,
        int memberFailed,
        int challengerFailed,
        ChallengerTrack track
    ) {

        public static PerCellSummary from(SeedChallengersResult.PerCellSummary summary) {
            return new PerCellSummary(
                summary.chapterId(),
                summary.schoolId(),
                summary.part(),
                summary.created(),
                summary.memberFailed(),
                summary.challengerFailed(),
                summary.track()
            );
        }
    }

    public static SeedChallengersResponse from(SeedChallengersResult result) {
        return new SeedChallengersResponse(
            result.gisuId(),
            result.totalCreated(),
            result.totalFailed(),
            result.perCellSummary().stream().map(PerCellSummary::from).toList()
        );
    }
}
