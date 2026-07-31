package com.umc.product.community.application.port.in.query.thread.dto;

import java.util.List;

public record GetThreadMembersByIdsQuery(
    Long threadId,
    Long requesterMemberId,
    List<Long> memberIds
) {

    public GetThreadMembersByIdsQuery {
        threadId = ThreadQueryConstraints.requirePositive(threadId, "threadId");
        requesterMemberId = ThreadQueryConstraints.requirePositive(requesterMemberId, "requesterMemberId");
        memberIds = ThreadQueryConstraints.requireUniquePositiveIds(memberIds, "memberIds");
    }
}
