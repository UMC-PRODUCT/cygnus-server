package com.umc.product.project.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

public record MemberBriefGraphQlResponse(
    String memberId,
    String nickname,
    String name,
    String schoolName
) {
    public static MemberBriefGraphQlResponse from(MemberInfo info) {
        return new MemberBriefGraphQlResponse(
            GlobalId.encode(GlobalIdTypes.MEMBER, info.id()),
            info.nickname(),
            info.name(),
            info.schoolName()
        );
    }
}
