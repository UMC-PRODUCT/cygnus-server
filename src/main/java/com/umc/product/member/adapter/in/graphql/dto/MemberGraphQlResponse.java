package com.umc.product.member.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

public record MemberGraphQlResponse(
    Long memberId,
    String name,
    String nickname,
    String email,
    Long schoolId,
    String schoolName,
    String profileImageLink,
    MemberStatus status
) {

    public static MemberGraphQlResponse privateFrom(MemberInfo memberInfo) {
        return new MemberGraphQlResponse(
            memberInfo.id(),
            memberInfo.name(),
            memberInfo.nickname(),
            memberInfo.email(),
            memberInfo.schoolId(),
            memberInfo.schoolName(),
            memberInfo.profileImageLink(),
            memberInfo.status()
        );
    }

    public static MemberGraphQlResponse publicFrom(MemberInfo memberInfo) {
        return new MemberGraphQlResponse(
            memberInfo.id(),
            memberInfo.name(),
            memberInfo.nickname(),
            null,
            memberInfo.schoolId(),
            memberInfo.schoolName(),
            memberInfo.profileImageLink(),
            null
        );
    }
}
