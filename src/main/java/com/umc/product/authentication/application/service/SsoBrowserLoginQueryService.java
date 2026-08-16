package com.umc.product.authentication.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.application.port.in.query.GetSsoBrowserLoginUseCase;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SsoBrowserLoginQueryService implements GetSsoBrowserLoginUseCase {

    private final SsoLoginTokenProvider tokenProvider;

    @Override
    @Transactional(readOnly = true)
    public SsoBrowserLoginInfo getLogin(String rawLoginToken) {
        SsoLoginTokenClaims claims = tokenProvider.parseLoginToken(rawLoginToken);
        if (!claims.expiresAt().isAfter(Instant.now())) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_BROWSER_LOGIN);
        }
        return SsoBrowserLoginInfo.of(claims.memberId(), rawLoginToken, claims.expiresAt());
    }
}
