package com.umc.product.member.application.port.out.dto;

import java.util.Set;

public record SearchMemberInvitationCondition(
    String keyword,
    Set<Long> excludedMemberIds,
    int offset,
    int limit
) {

    public SearchMemberInvitationCondition {
        excludedMemberIds = excludedMemberIds == null ? Set.of() : Set.copyOf(excludedMemberIds);
    }
}
