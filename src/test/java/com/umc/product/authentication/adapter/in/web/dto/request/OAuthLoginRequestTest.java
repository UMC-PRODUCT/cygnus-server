package com.umc.product.authentication.adapter.in.web.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ClientType;

@DisplayName("OAuth 로그인 요청 DTO")
class OAuthLoginRequestTest {

    @Test
    @DisplayName("Google 로그인 요청은 idToken을 accessToken보다 우선 사용한다")
    void google_id_token_preferred() {
        GoogleLoginRequest request = new GoogleLoginRequest("id-token", "access-token", ClientType.WEB);

        assertThat(request.token()).isEqualTo("id-token");
    }

    @Test
    @DisplayName("Google 로그인 요청은 idToken이 없으면 기존 accessToken을 사용한다")
    void google_access_token_fallback() {
        GoogleLoginRequest request = new GoogleLoginRequest(null, "access-token", ClientType.WEB);

        assertThat(request.token()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("Kakao 로그인 요청은 idToken을 accessToken보다 우선 사용한다")
    void kakao_id_token_preferred() {
        KakaoLoginRequest request = new KakaoLoginRequest("id-token", "access-token", ClientType.WEB);

        assertThat(request.token()).isEqualTo("id-token");
    }

    @Test
    @DisplayName("Kakao 로그인 요청은 idToken이 없으면 기존 accessToken을 사용한다")
    void kakao_access_token_fallback() {
        KakaoLoginRequest request = new KakaoLoginRequest(null, "access-token", ClientType.WEB);

        assertThat(request.token()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("Google 로그인 요청은 idToken과 accessToken이 모두 비어 있으면 검증 실패한다")
    void google_token_required() {
        assertThatThrownBy(() -> new GoogleLoginRequest(" ", null, ClientType.WEB))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("idToken 또는 accessToken 중 하나는 필수입니다.");
    }

    @Test
    @DisplayName("Kakao 로그인 요청은 idToken과 accessToken이 모두 비어 있으면 검증 실패한다")
    void kakao_token_required() {
        assertThatThrownBy(() -> new KakaoLoginRequest(null, " ", ClientType.WEB))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("idToken 또는 accessToken 중 하나는 필수입니다.");
    }
}
