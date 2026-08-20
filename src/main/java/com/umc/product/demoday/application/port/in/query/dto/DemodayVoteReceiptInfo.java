package com.umc.product.demoday.application.port.in.query.dto;

import java.time.Instant;

public record DemodayVoteReceiptInfo(
    Long voteId,
    DemodayBoothInfo selectedBooth,
    Instant votedAt
) {
}
