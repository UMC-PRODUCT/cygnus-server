package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.member.application.port.in.query.dto.MemberInfo;

public record MemberBriefGraphQlResponse(
    Long memberId,
    String nickname,
    String name,
    String schoolName
) {
    public static MemberBriefGraphQlResponse from(MemberInfo info) {
        return new MemberBriefGraphQlResponse(
            info.id(),
            info.nickname(),
            info.name(),
            info.schoolName()
        );
    }
}
