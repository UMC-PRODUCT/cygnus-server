package com.umc.product.support.fixture;

import java.util.Map;

import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.port.in.command.dto.LinkOAuthCommand;
import com.umc.product.authentication.application.port.in.command.dto.UnlinkOAuthCommand;
import com.umc.product.authentication.domain.MemberOAuth;
import com.umc.product.authentication.domain.OAuthAttributes;
import com.umc.product.common.domain.enums.OAuthProvider;

public final class AuthenticationFixture {

    private AuthenticationFixture() {
    }

    public static MemberOAuth OAuth_계정(
        Long id,
        Long memberId,
        OAuthProvider provider,
        String providerId
    ) {
        return OAuth_계정(id, memberId, provider, providerId, null, null);
    }

    public static MemberOAuth OAuth_계정(
        Long id,
        Long memberId,
        OAuthProvider provider,
        String providerId,
        String appleRefreshToken,
        String appleClientId
    ) {
        MemberOAuth memberOAuth = MemberOAuth.builder()
            .memberId(memberId)
            .provider(provider)
            .providerId(providerId)
            .appleRefreshToken(appleRefreshToken)
            .appleClientId(appleClientId)
            .build();
        ReflectionTestUtils.setField(memberOAuth, "id", id);
        return memberOAuth;
    }

    public static LinkOAuthCommand OAuth_연결_명령(
        Long memberId,
        OAuthProvider provider,
        String providerId
    ) {
        return LinkOAuthCommand.builder()
            .memberId(memberId)
            .provider(provider)
            .providerId(providerId)
            .build();
    }

    public static LinkOAuthCommand Apple_OAuth_연결_명령(
        Long memberId,
        String providerId,
        String refreshToken,
        String clientId
    ) {
        return LinkOAuthCommand.builder()
            .memberId(memberId)
            .provider(OAuthProvider.APPLE)
            .providerId(providerId)
            .appleRefreshToken(refreshToken)
            .appleClientId(clientId)
            .build();
    }

    public static UnlinkOAuthCommand OAuth_해제_명령(
        Long memberId,
        Long memberOAuthId,
        boolean withdrawal,
        String googleAccessToken,
        String kakaoAccessToken
    ) {
        return UnlinkOAuthCommand.builder()
            .memberId(memberId)
            .memberOAuthId(memberOAuthId)
            .isWithdrawal(withdrawal)
            .googleAccessToken(googleAccessToken)
            .kakaoAccessToken(kakaoAccessToken)
            .build();
    }

    public static OAuthAttributes OAuth_속성(
        OAuthProvider provider,
        String providerId,
        String email
    ) {
        Map<String, Object> attributes = switch (provider) {
            case GOOGLE -> Map.of("sub", providerId, "email", email);
            case APPLE -> Map.of("sub", providerId, "email", email);
            case KAKAO -> Map.of(
                "id", providerId,
                "kakao_account", Map.of("email", email)
            );
        };
        return OAuthAttributes.of(provider.name(), attributes);
    }
}
