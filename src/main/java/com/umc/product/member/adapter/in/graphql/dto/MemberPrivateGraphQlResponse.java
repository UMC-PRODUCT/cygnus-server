package com.umc.product.member.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

public record MemberPrivateGraphQlResponse(
    String email,
    MemberStatus status
) {

    public static MemberPrivateGraphQlResponse from(MemberInfo info) {
        return new MemberPrivateGraphQlResponse(info.email(), info.status());
    }
}
