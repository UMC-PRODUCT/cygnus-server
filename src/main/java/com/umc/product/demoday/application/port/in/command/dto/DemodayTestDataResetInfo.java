package com.umc.product.demoday.application.port.in.command.dto;

import com.umc.product.demoday.application.port.out.dto.DemodayTestDataDeletionCounts;

public record DemodayTestDataResetInfo(
    int deletedPolls,
    int deletedBooths,
    int deletedEntryCodes,
    int deletedStamps,
    int deletedVotes
) {

    public static DemodayTestDataResetInfo from(DemodayTestDataDeletionCounts counts) {
        return new DemodayTestDataResetInfo(
            counts.deletedPolls(),
            counts.deletedBooths(),
            counts.deletedEntryCodes(),
            counts.deletedStamps(),
            counts.deletedVotes()
        );
    }
}
