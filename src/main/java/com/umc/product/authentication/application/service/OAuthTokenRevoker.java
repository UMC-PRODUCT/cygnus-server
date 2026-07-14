package com.umc.product.authentication.application.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authentication.application.port.in.command.dto.UnlinkOAuthCommand;
import com.umc.product.authentication.application.port.out.RevokeOAuthTokenPort;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.authentication.domain.MemberOAuth;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class OAuthTokenRevoker {

    private final RevokeOAuthTokenPort revokeOAuthTokenPort;
    private final VerifyOAuthTokenPort verifyOAuthTokenPort;

    void revoke(MemberOAuth memberOAuth, UnlinkOAuthCommand command) {
        switch (memberOAuth.getProvider()) {
            case APPLE -> revokeApple(memberOAuth);
            case KAKAO -> revokeKakao(memberOAuth, command.kakaoAccessToken());
            case GOOGLE -> revokeGoogle(memberOAuth, command.googleAccessToken());
        }
    }

    private void revokeApple(MemberOAuth memberOAuth) {
        if (memberOAuth.getAppleRefreshToken() != null && memberOAuth.getAppleClientId() != null) {
            revokeOAuthTokenPort.revokeAppleToken(
                memberOAuth.getAppleRefreshToken(),
                memberOAuth.getAppleClientId()
            );
            return;
        }
        log.warn("[Apple 계정 연동 해제] revoke credential이 없습니다: memberId={} memberOAuthId={}",
            memberOAuth.getMemberId(), memberOAuth.getId());
    }

    private void revokeKakao(MemberOAuth memberOAuth, String accessToken) {
        if (accessToken == null) {
            log.warn("[Kakao 계정 연동 해제] access token이 없습니다: memberId={} memberOAuthId={}",
                memberOAuth.getMemberId(), memberOAuth.getId());
            return;
        }
        validateAccessTokenOwner(memberOAuth, accessToken);
        revokeOAuthTokenPort.revokeKakaoToken(accessToken);
    }

    private void revokeGoogle(MemberOAuth memberOAuth, String accessToken) {
        if (accessToken == null) {
            log.warn("[Google 계정 연동 해제] access token이 없습니다: memberId={} memberOAuthId={}",
                memberOAuth.getMemberId(), memberOAuth.getId());
            return;
        }
        validateAccessTokenOwner(memberOAuth, accessToken);
        revokeOAuthTokenPort.revokeGoogleToken(accessToken);
    }

    private void validateAccessTokenOwner(MemberOAuth memberOAuth, String accessToken) {
        OAuthAttributes tokenAttributes = verifyOAuthTokenPort.verify(memberOAuth.getProvider(), accessToken);
        if (tokenAttributes.provider() != memberOAuth.getProvider()
            || !Objects.equals(tokenAttributes.providerId(), memberOAuth.getProviderId())) {
            log.warn("[{} 계정 연동 해제] access token 소유자가 일치하지 않습니다: memberId={} memberOAuthId={}",
                memberOAuth.getProvider(), memberOAuth.getMemberId(), memberOAuth.getId());
            throw new AuthenticationDomainException(AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN);
        }
    }
}
