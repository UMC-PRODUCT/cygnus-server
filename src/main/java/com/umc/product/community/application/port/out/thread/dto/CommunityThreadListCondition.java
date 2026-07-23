package com.umc.product.community.application.port.out.thread.dto;

import com.umc.product.community.domain.enums.CommunityThreadCategory;

public record CommunityThreadListCondition(
    Long requesterMemberId,
    CommunityThreadCategory category,
    boolean unreadOnly,
    String keyword,
    int offset,
    int limit
) {
}
