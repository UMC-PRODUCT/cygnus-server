package com.umc.product.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.product.common.domain.enums.ClientType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@DisplayName("MemberPrincipal")
class MemberPrincipalTest {

    @Test
    @DisplayName("인증 토큰의 name은 MemberPrincipal의 회원 ID 문자열이다")
    void authentication_name_is_member_id() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(1L)
            .clientType(ClientType.WEB)
            .build();
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        assertThat(principal.getName()).isEqualTo("1");
        assertThat(authentication.getName()).isEqualTo("1");
    }
}
