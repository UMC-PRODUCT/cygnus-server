package com.umc.product.demoday.application.port.out.dto;

public record DemodayTestDataDeletionCounts(
    int deletedPolls,
    int deletedBooths,
    int deletedEntryCodes,
    int deletedStamps,
    int deletedVotes
) {
}
