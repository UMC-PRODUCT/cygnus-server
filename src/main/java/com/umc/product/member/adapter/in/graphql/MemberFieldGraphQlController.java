package com.umc.product.member.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.member.adapter.in.graphql.dto.MemberPrivateGraphQlResponse;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberFieldGraphQlController {

    private final CurrentMemberProvider currentMemberProvider;
    private final GetMemberUseCase getMemberUseCase;

    @SchemaMapping(typeName = "MemberPublic", field = "private")
    public MemberPrivateGraphQlResponse privateInfo(MemberPublicInfo member) {
        if (!java.util.Objects.equals(currentMemberProvider.getNullableCurrentMemberId(), member.memberId())) {
            return null;
        }
        return MemberPrivateGraphQlResponse.from(getMemberUseCase.getById(member.memberId()));
    }
}
