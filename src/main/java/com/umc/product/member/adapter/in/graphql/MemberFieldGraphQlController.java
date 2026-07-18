package com.umc.product.member.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberFieldGraphQlController {

    private final CurrentMemberProvider currentMemberProvider;

    @SchemaMapping(typeName = "Member", field = "memberId")
    public Long memberId(MemberInfo member) {
        return member.id();
    }

    @SchemaMapping(typeName = "Member", field = "email")
    public String email(MemberInfo member) {
        return isRequester(member) ? member.email() : null;
    }

    @SchemaMapping(typeName = "Member", field = "status")
    public MemberStatus status(MemberInfo member) {
        return isRequester(member) ? member.status() : null;
    }

    private boolean isRequester(MemberInfo member) {
        return currentMemberProvider.getRequiredCurrentMemberId().equals(member.id());
    }
}
