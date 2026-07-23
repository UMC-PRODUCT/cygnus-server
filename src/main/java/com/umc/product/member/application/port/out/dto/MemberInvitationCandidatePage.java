package com.umc.product.member.application.port.out.dto;

import java.util.List;

public record MemberInvitationCandidatePage(
    List<MemberInvitationCandidate> items,
    long total
) {

    public MemberInvitationCandidatePage {
        items = items == null ? List.of() : List.copyOf(items);
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }
}
