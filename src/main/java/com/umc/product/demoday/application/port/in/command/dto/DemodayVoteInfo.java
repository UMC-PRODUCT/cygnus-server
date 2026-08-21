package com.umc.product.demoday.application.port.in.command.dto;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;

public record DemodayVoteInfo(
    Long voteId,
    Long pollId,
    DemodayBoothInfo selectedBooth,
    Instant votedAt
) {
}
