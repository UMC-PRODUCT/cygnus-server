package com.umc.product.community.application.port.in.query.thread.dto;

public record SearchThreadInvitableQuery(
    Long threadId,
    Long requesterMemberId,
    String q,
    int offset,
    int limit
) {

    public SearchThreadInvitableQuery {
        threadId = ThreadQueryConstraints.requirePositive(threadId, "threadId");
        requesterMemberId = ThreadQueryConstraints.requirePositive(requesterMemberId, "requesterMemberId");
        q = ThreadQueryConstraints.normalizeKeyword(q);
        ThreadQueryConstraints.requirePage(offset, limit);
    }
}
