package com.umc.product.authentication.application.port.in.dto;

import java.time.Instant;

import com.umc.product.authentication.domain.enums.OAuthResultCode;
import com.umc.product.common.domain.enums.OAuthProvider;

public record SsoBrowserOAuthLoginResult(
    OAuthProvider provider,
    String code,
    Long memberId,
    String loginToken,
    Instant expiresAt,
    String oAuthVerificationToken
) {
    public static SsoBrowserOAuthLoginResult loginSuccess(
        OAuthProvider provider,
        SsoBrowserLoginInfo loginInfo
    ) {
        return new SsoBrowserOAuthLoginResult(
            provider,
            OAuthResultCode.SUCCESS.getCode(),
            loginInfo.memberId(),
            loginInfo.loginToken(),
            loginInfo.expiresAt(),
            null
        );
    }

    public static SsoBrowserOAuthLoginResult registerRequired(
        OAuthProvider provider,
        String oAuthVerificationToken
    ) {
        return new SsoBrowserOAuthLoginResult(
            provider,
            OAuthResultCode.REGISTER_REQUIRED.getCode(),
            null,
            null,
            null,
            oAuthVerificationToken
        );
    }

    public boolean hasLoginCookie() {
        return loginToken != null && expiresAt != null;
    }
}
