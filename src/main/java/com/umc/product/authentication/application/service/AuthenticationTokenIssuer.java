package com.umc.product.authentication.application.service;

import java.time.Duration;
import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.out.SaveRefreshTokenPort;
import com.umc.product.authentication.domain.RefreshToken;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.RefreshTokenClaims;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationTokenIssuer {

    private final JwtTokenProvider jwtTokenProvider;
    private final SaveRefreshTokenPort saveRefreshTokenPort;

    @Transactional
    public NewTokens issue(Long memberId, ClientType clientType) {
        String accessToken = jwtTokenProvider.createAccessToken(memberId, Collections.emptyList(), clientType);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);
        RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);

        saveRefreshTokenPort.save(RefreshToken.create(
            claims.jti(),
            claims.memberId(),
            claims.expiresAt()
        ));

        return NewTokens.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    @Transactional
    public NewTokens issue(
        Long memberId,
        ClientType clientType,
        ClientContextClaims clientContext,
        Duration accessTokenTtl
    ) {
        long expiresIn = accessTokenTtl.toSeconds();
        String accessToken = jwtTokenProvider.createAccessToken(
            memberId,
            Collections.emptyList(),
            clientType,
            clientContext,
            expiresIn
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId, clientContext);
        RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);

        saveRefreshTokenPort.save(RefreshToken.create(
            claims.jti(),
            claims.memberId(),
            claims.expiresAt(),
            claims.clientContext().clientId()
        ));

        return NewTokens.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(expiresIn)
            .clientContextClaims(claims.clientContext())
            .build();
    }
}
