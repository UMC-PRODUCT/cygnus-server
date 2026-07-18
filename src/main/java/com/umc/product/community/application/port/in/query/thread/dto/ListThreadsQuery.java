package com.umc.product.community.application.port.in.query.thread.dto;

public record ListThreadsQuery(
    Long requesterMemberId,
    ThreadListFilter filter,
    String q,
    int offset,
    int limit
) {

    public ListThreadsQuery {
        requesterMemberId = ThreadQueryConstraints.requirePositive(requesterMemberId, "requesterMemberId");
        if (filter == null) {
            throw new IllegalArgumentException("filter must not be null");
        }
        q = ThreadQueryConstraints.normalizeKeyword(q);
        ThreadQueryConstraints.requirePage(offset, limit);
    }
}
