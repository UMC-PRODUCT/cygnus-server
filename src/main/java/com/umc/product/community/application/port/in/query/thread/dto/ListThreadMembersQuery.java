package com.umc.product.community.application.port.in.query.thread.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;

public record ListThreadMembersQuery(
    Long threadId,
    Long requesterMemberId,
    String q,
    CommunityThreadMemberRole role,
    ChallengerPart part,
    Long generation,
    int offset,
    int limit
) {

    public ListThreadMembersQuery {
        threadId = ThreadQueryConstraints.requirePositive(threadId, "threadId");
        requesterMemberId = ThreadQueryConstraints.requirePositive(requesterMemberId, "requesterMemberId");
        q = ThreadQueryConstraints.normalizeKeyword(q);
        if (generation != null && generation <= 0) {
            throw new IllegalArgumentException("generation must be positive");
        }
        ThreadQueryConstraints.requirePage(offset, limit);
    }
}
