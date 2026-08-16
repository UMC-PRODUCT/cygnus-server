package com.umc.product.authentication.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.authentication.application.port.in.dto.SsoBrowserOAuthLoginResult;
import com.umc.product.common.domain.enums.OAuthProvider;

public record SsoSocialLoginResponse(
    OAuthProvider provider,
    String code,
    Long memberId,
    Instant expiresAt,
    String oAuthVerificationToken
) {
    public static SsoSocialLoginResponse from(SsoBrowserOAuthLoginResult result) {
        return new SsoSocialLoginResponse(
            result.provider(),
            result.code(),
            result.memberId(),
            result.expiresAt(),
            result.oAuthVerificationToken()
        );
    }
}
