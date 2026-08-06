package com.umc.product.community.application.port.in.query.thread.dto;

import com.umc.product.common.domain.enums.ChallengerPart;

public record ThreadInvitableInfo(
    Long memberId,
    Long challengerId,
    String name,
    ChallengerPart part,
    Long generation
) {
}
