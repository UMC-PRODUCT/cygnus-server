package com.umc.product.demoday.adapter.in.web.dto;

public record CreateDemodayPollResponse(Long pollId) {

    public static CreateDemodayPollResponse from(Long pollId) {
        return new CreateDemodayPollResponse(pollId);
    }
}
