package com.umc.product.member.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@Controller
public class MemberFieldGraphQlController {

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
        return currentMemberId().equals(member.id());
    }

    private Long currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요해요. 로그인 후 다시 시도해주세요.");
        }
        if (authentication.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }
        throw new AccessDeniedException("인증 정보가 올바르지 않아요. 다시 로그인해주세요.");
    }
}
