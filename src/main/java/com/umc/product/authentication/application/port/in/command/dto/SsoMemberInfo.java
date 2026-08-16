package com.umc.product.authentication.application.port.in.command.dto;

import com.umc.product.member.application.port.in.query.dto.MemberInfo;

public record SsoMemberInfo(
    Long id,
    String name,
    String nickname,
    String email
) {
    public static SsoMemberInfo of(Long id, String name, String nickname, String email) {
        return new SsoMemberInfo(id, name, nickname, email);
    }

    public static SsoMemberInfo from(MemberInfo memberInfo) {
        return new SsoMemberInfo(
            memberInfo.id(),
            memberInfo.name(),
            memberInfo.nickname(),
            memberInfo.email()
        );
    }
}
