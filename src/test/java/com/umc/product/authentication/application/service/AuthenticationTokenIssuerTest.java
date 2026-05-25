package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
    void 필수_약관_동의_상태_claim_반영과_refresh_token_저장() {
        // given
        given(getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(MEMBER_ID))
            .willReturn(new RequiredTermConsentStatusInfo(true, List.of(), List.of(10L)));
        given(jwtTokenProvider.createAccessToken(
            eq(MEMBER_ID),
            anyList(),
            eq(ClientType.IOS),
            eq(false),
            eq(List.of(10L))
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
}
