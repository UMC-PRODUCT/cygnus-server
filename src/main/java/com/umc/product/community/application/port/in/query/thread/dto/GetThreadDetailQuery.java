package com.umc.product.community.application.port.in.query.thread.dto;

public record GetThreadDetailQuery(Long threadId, Long requesterMemberId) {

    public GetThreadDetailQuery {
        threadId = ThreadQueryConstraints.requirePositive(threadId, "threadId");
        requesterMemberId = ThreadQueryConstraints.requirePositive(requesterMemberId, "requesterMemberId");
    }
}
