package com.umc.product.test.application.port.in.command.dto;

import com.umc.product.member.domain.MemberSystemRoleType;

public record CreateSeedMemberSystemRoleCommand(
    Long memberId,
    MemberSystemRoleType roleType
) {
}
