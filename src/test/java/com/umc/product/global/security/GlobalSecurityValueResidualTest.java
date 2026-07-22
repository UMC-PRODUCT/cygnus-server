package com.umc.product.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextClaims;

@DisplayName("global security 값과 현재 회원 잔여 계약")
class GlobalSecurityValueResidualTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보 부재·미인증·다른 principal은 nullable 회원과 ID를 null로 반환한다")
    void 현재_회원_부재를_처리한다() {
        CurrentMemberProvider sut = new CurrentMemberProvider();
        assertThat(sut.getNullableCurrentMember()).isNull();
        assertThat(sut.getNullableCurrentMemberId()).isNull();

        UsernamePasswordAuthenticationToken unauthenticated =
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(1L), null);
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);
        assertThat(sut.getNullableCurrentMember()).isNull();

        UsernamePasswordAuthenticationToken otherPrincipal =
            new UsernamePasswordAuthenticationToken("member", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(otherPrincipal);
        assertThat(sut.getNullableCurrentMember()).isNull();
        assertThatThrownBy(sut::getRequiredCurrentMember).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("인증 회원의 nullable ID와 principal 문자열은 모든 client context를 보존한다")
    void 현재_회원_ID와_문자열을_반환한다() {
        MemberPrincipal principal = new MemberPrincipal(10L, ClientType.IOS, ClientContextClaims.empty());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));

        assertThat(new CurrentMemberProvider().getNullableCurrentMemberId()).isEqualTo(10L);
        assertThat(principal.toString()).contains("memberId=10", "clientType=IOS", "clientContextClaims=");
    }

    @Test
    @DisplayName("RefreshTokenClaims 보조 생성자와 null context는 빈 context로 정규화한다")
    void RefreshTokenClaims_context를_정규화한다() {
        UUID jti = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-22T00:00:00Z");

        assertThat(new RefreshTokenClaims(1L, jti, expiresAt).clientContext())
            .isEqualTo(ClientContextClaims.empty());
        assertThat(new RefreshTokenClaims(1L, jti, expiresAt, null).clientContext())
            .isEqualTo(ClientContextClaims.empty());
    }
}
