package com.umc.product.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentMemberProviderTest {

    CurrentMemberProvider sut = new CurrentMemberProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 MemberPrincipal을 현재 회원으로 반환한다")
    void 인증된_MemberPrincipal을_현재_회원으로_반환한다() {
        // Given
        MemberPrincipal principal = new MemberPrincipal(10L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        // When
        MemberPrincipal result = sut.getNullableCurrentMember();

        // Then
        assertThat(result).isSameAs(principal);
        assertThat(sut.getRequiredCurrentMemberId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("익명 사용자는 nullable 현재 회원에서 null로 반환한다")
    void 익명_사용자는_nullable_현재_회원에서_null로_반환한다() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        // When & Then
        assertThat(sut.getNullableCurrentMember()).isNull();
        assertThatThrownBy(() -> sut.getRequiredCurrentMember())
            .isInstanceOf(AccessDeniedException.class);
    }
}
