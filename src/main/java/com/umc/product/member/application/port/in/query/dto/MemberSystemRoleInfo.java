package com.umc.product.member.application.port.in.query.dto;

import com.umc.product.member.domain.MemberSystemRole;

public record MemberSystemRoleInfo(
    Long memberId,
    String roleType
) {

    public static MemberSystemRoleInfo from(MemberSystemRole role) {
        return new MemberSystemRoleInfo(role.getMemberId(), role.getRoleType().name());
    }
}
