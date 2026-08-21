package com.umc.product.test.application.port.in.command.dto;

import com.umc.product.member.domain.MemberSystemRoleType;

public record CreateSeedMemberSystemRoleResult(
    Long memberId,
    MemberSystemRoleType roleType,
    boolean created
) {

    public static CreateSeedMemberSystemRoleResult of(
        Long memberId,
        MemberSystemRoleType roleType,
        boolean created
    ) {
        return new CreateSeedMemberSystemRoleResult(memberId, roleType, created);
    }
}
