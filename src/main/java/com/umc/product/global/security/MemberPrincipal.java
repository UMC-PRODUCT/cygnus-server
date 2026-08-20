package com.umc.product.global.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextClaims;

import lombok.Builder;
import lombok.Getter;

@Getter
public class MemberPrincipal implements RateLimitPrincipal {

    private final Long memberId;

    // AT claim 으로 전달된 클라이언트 플랫폼. 도입 이전 토큰 / claim 누락 토큰은 null.
    // 통계/로그 컨텍스트용 메타데이터이며, 인가 결정에는 영향을 주지 않는다.
    private final ClientType clientType;

    private final ClientContextClaims clientContextClaims;

    @Builder
    public MemberPrincipal(Long memberId, ClientType clientType, ClientContextClaims clientContextClaims) {
        this.memberId = memberId;
        this.clientType = clientType;
        this.clientContextClaims = clientContextClaims == null ? ClientContextClaims.empty() : clientContextClaims;
    }

    public MemberPrincipal(Long memberId) {
        this(memberId, null);
    }

    public MemberPrincipal(Long memberId, ClientType clientType) {
        this(memberId, clientType, ClientContextClaims.empty());
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String rateLimitKey() {
        return "member:" + memberId;
    }

    @Override
    public String rateLimitClientType() {
        return clientType == null ? "UNKNOWN" : clientType.name();
    }

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }

    @Override
    public String toString() {
        return "MemberPrincipal{"
            + "memberId=" + memberId
            + ", clientType=" + clientType
            + ", clientContextClaims=" + clientContextClaims
            + '}';
    }
}
