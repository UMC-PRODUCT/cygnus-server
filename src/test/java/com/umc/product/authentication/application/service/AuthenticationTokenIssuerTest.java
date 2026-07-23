package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.out.SaveRefreshTokenPort;
import com.umc.product.authentication.domain.RefreshToken;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.RefreshTokenClaims;
import com.umc.product.term.application.port.in.query.GetRequiredTermConsentStatusUseCase;
import com.umc.product.term.application.port.in.query.dto.RequiredTermConsentStatusInfo;

@ExtendWith(MockitoExtension.class)
class AuthenticationTokenIssuerTest {

    private static final Long MEMBER_ID = 1L;
    private static final UUID REFRESH_JTI = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant REFRESH_EXPIRES_AT = Instant.parse("2026-06-24T00:00:00Z");

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    SaveRefreshTokenPort saveRefreshTokenPort;

    @Mock
    GetRequiredTermConsentStatusUseCase getRequiredTermConsentStatusUseCase;

    @InjectMocks
    AuthenticationTokenIssuer issuer;

    @Test
    @DisplayName("토큰 발급 시 최신 필수 약관 동의 상태를 AccessToken claim에 반영하고 RefreshToken을 저장한다")
    void issueTokenWithRequiredTermSnapshot() {
        // given
        given(getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(MEMBER_ID))
            .willReturn(new RequiredTermConsentStatusInfo(true, List.of()));
        given(jwtTokenProvider.createAccessToken(
            eq(MEMBER_ID),
            anyList(),
            eq(ClientType.IOS),
            eq(false)
        )).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(MEMBER_ID)).willReturn("refresh-token");
        given(jwtTokenProvider.parseRefreshToken("refresh-token"))
            .willReturn(new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT));

        // when
        NewTokens result = issuer.issue(MEMBER_ID, ClientType.IOS);

        // then
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        then(saveRefreshTokenPort).should().save(refreshTokenCaptor.capture());
        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();
        assertThat(savedRefreshToken.getJti()).isEqualTo(REFRESH_JTI);
        assertThat(savedRefreshToken.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(REFRESH_EXPIRES_AT);
    }

    @Test
    @DisplayName("일반 토큰 발급은 필수 약관 동의 완료 상태도 true claim으로 반영한다")
    void issueGeneralTokenWithAgreedSnapshot() {
        given(getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(MEMBER_ID))
            .willReturn(new RequiredTermConsentStatusInfo(false, List.of()));
        given(jwtTokenProvider.createAccessToken(MEMBER_ID, List.of(), ClientType.ANDROID, true))
            .willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(MEMBER_ID)).willReturn("refresh-token");
        given(jwtTokenProvider.parseRefreshToken("refresh-token"))
            .willReturn(new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT));

        NewTokens result = issuer.issue(MEMBER_ID, ClientType.ANDROID);

        assertThat(result.accessToken()).isEqualTo("access-token");
        then(jwtTokenProvider).should().createAccessToken(MEMBER_ID, List.of(), ClientType.ANDROID, true);
    }

    @Test
    @DisplayName("SSO 토큰 발급도 client context와 필수 약관 동의 snapshot을 함께 보존한다")
    void issueSsoTokenWithClientContextAndTermSnapshot() {
        ClientContextClaims clientContext = ClientContextClaims.of(
            "backoffice",
            ClientServiceType.UMC_BACKOFFICE,
            ClientEnvironment.PROD
        );
        Duration accessTokenTtl = Duration.ofHours(1);
        given(getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(MEMBER_ID))
            .willReturn(new RequiredTermConsentStatusInfo(false, List.of()));
        given(jwtTokenProvider.createAccessToken(
            MEMBER_ID,
            List.of(),
            ClientType.WEB,
            clientContext,
            true,
            accessTokenTtl
        )).willReturn("sso-access-token");
        given(jwtTokenProvider.createRefreshToken(MEMBER_ID, clientContext)).willReturn("sso-refresh-token");
        given(jwtTokenProvider.parseRefreshToken("sso-refresh-token"))
            .willReturn(new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT, clientContext));

        NewTokens result = issuer.issue(MEMBER_ID, ClientType.WEB, clientContext, accessTokenTtl);

        assertThat(result.accessToken()).isEqualTo("sso-access-token");
        assertThat(result.clientContextClaims()).isEqualTo(clientContext);
        assertThat(result.expiresIn()).isEqualTo(3600L);
        then(jwtTokenProvider).should().createAccessToken(
            MEMBER_ID,
            List.of(),
            ClientType.WEB,
            clientContext,
            true,
            accessTokenTtl
        );
    }

    @Test
    @DisplayName("SSO 토큰 발급은 client context를 보존하면서 미동의 상태를 false claim으로 반영한다")
    void issueSsoTokenWithReconsentSnapshot() {
        ClientContextClaims clientContext = ClientContextClaims.of(
            "backoffice",
            ClientServiceType.UMC_BACKOFFICE,
            ClientEnvironment.PROD
        );
        Duration accessTokenTtl = Duration.ofHours(1);
        given(getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(MEMBER_ID))
            .willReturn(new RequiredTermConsentStatusInfo(true, List.of()));
        given(jwtTokenProvider.createAccessToken(
            MEMBER_ID,
            List.of(),
            ClientType.WEB,
            clientContext,
            false,
            accessTokenTtl
        )).willReturn("sso-access-token");
        given(jwtTokenProvider.createRefreshToken(MEMBER_ID, clientContext)).willReturn("sso-refresh-token");
        given(jwtTokenProvider.parseRefreshToken("sso-refresh-token"))
            .willReturn(new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT, clientContext));

        NewTokens result = issuer.issue(MEMBER_ID, ClientType.WEB, clientContext, accessTokenTtl);

        assertThat(result.accessToken()).isEqualTo("sso-access-token");
        assertThat(result.clientContextClaims()).isEqualTo(clientContext);
        then(jwtTokenProvider).should().createAccessToken(
            MEMBER_ID,
            List.of(),
            ClientType.WEB,
            clientContext,
            false,
            accessTokenTtl
        );
    }
}
