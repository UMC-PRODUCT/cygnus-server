package com.umc.product.demoday.adapter.in.web.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;

import com.umc.product.global.security.RateLimitPrincipal;

/**
 * 게스트(외부 방문자) 참여 인증 principal.
 * <p>
 * 회원 인증({@code MemberPrincipal})과는 별개의 경로다 — 기존 인증 코어를 수정하지 않고 게스트
 * 인증을 완전히 독립된 Principal 타입으로 분리했다. 신원은 입장 코드 단위({@code entryCodeId})로만
 * 식별되며, 이 값은 {@link DemodayParticipantTokenProvider}가 서명한 participant token의 subject다.
 */
public class DemodayParticipationPrincipal implements RateLimitPrincipal {

    private final Long entryCodeId;

    public DemodayParticipationPrincipal(Long entryCodeId) {
        this.entryCodeId = Objects.requireNonNull(entryCodeId, "entryCodeId must not be null");
    }

    public Long getEntryCodeId() {
        return entryCodeId;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String rateLimitKey() {
        return "guest:" + entryCodeId;
    }

    @Override
    public String rateLimitClientType() {
        return "GUEST";
    }

    @Override
    public String getName() {
        return String.valueOf(entryCodeId);
    }

    @Override
    public String toString() {
        return "DemodayParticipationPrincipal{entryCodeId=" + entryCodeId + '}';
    }
}
