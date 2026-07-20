package com.umc.product.authentication.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.adapter.in.web.dto.request.AddOAuthRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.UnlinkOAuthRequest;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.LinkOAuthCommand;
import com.umc.product.authentication.application.port.in.command.dto.UnlinkOAuthCommand;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authentication.application.port.in.query.dto.MemberOAuthInfo;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.OAuthVerificationClaims;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberOAuthController")
class MemberOAuthControllerUnitTest {

    @Mock
    OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    @Mock
    GetMemberOAuthUseCase oAuthListUseCase;
    @Mock
    JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    MemberOAuthController sut;

    @Test
    @DisplayName("검증 토큰의 provider 계정을 현재 회원에게 연결하고 최신 목록을 반환한다")
    void OAuth_계정을_연결한다() {
        MemberPrincipal principal = new MemberPrincipal(1L);
        OAuthVerificationClaims claims = new OAuthVerificationClaims(
            "member@example.com", OAuthProvider.GOOGLE, "provider-id");
        List<MemberOAuthInfo> infos = List.of(info());
        given(jwtTokenProvider.parseOAuthVerificationToken("verification-token")).willReturn(claims);
        given(oAuthListUseCase.getOAuthList(1L)).willReturn(infos);

        List<MemberOAuthInfo> result = sut.addMemberOAuth(
            principal, new AddOAuthRequest("verification-token"));

        then(oAuthAuthenticationUseCase).should().linkOAuth(LinkOAuthCommand.builder()
            .memberId(1L)
            .provider(OAuthProvider.GOOGLE)
            .providerId("provider-id")
            .build());
        assertThat(result).isSameAs(infos);
    }

    @Test
    @DisplayName("provider token이 주어지면 외부 연결 해제 정보와 함께 OAuth를 제거한다")
    void provider_token과_OAuth를_제거한다() {
        given(oAuthListUseCase.getOAuthList(1L)).willReturn(List.of());

        List<MemberOAuthInfo> result = sut.deleteMemberOAuth(
            new MemberPrincipal(1L),
            10L,
            new UnlinkOAuthRequest("google-token", "kakao-token")
        );

        then(oAuthAuthenticationUseCase).should().unlinkOAuth(UnlinkOAuthCommand.builder()
            .memberId(1L)
            .memberOAuthId(10L)
            .googleAccessToken("google-token")
            .kakaoAccessToken("kakao-token")
            .build());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("요청 body가 없으면 provider token 없이 OAuth를 제거한다")
    void body_없이_OAuth를_제거한다() {
        sut.deleteMemberOAuth(new MemberPrincipal(1L), 10L, null);

        then(oAuthAuthenticationUseCase).should().unlinkOAuth(UnlinkOAuthCommand.builder()
            .memberId(1L)
            .memberOAuthId(10L)
            .googleAccessToken(null)
            .kakaoAccessToken(null)
            .build());
    }

    @Test
    @DisplayName("현재 회원의 OAuth 목록을 조회한다")
    void 내_OAuth_목록을_조회한다() {
        List<MemberOAuthInfo> infos = List.of(info());
        given(oAuthListUseCase.getOAuthList(1L)).willReturn(infos);

        List<MemberOAuthInfo> result = sut.getMyOAuthInfos(new MemberPrincipal(1L));

        assertThat(result).isSameAs(infos);
    }

    private MemberOAuthInfo info() {
        return MemberOAuthInfo.builder()
            .memberOAuthId(10L)
            .memberId(1L)
            .provider(OAuthProvider.GOOGLE)
            .build();
    }
}
