package com.umc.product.authentication.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.umc.product.global.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SsoLoginTokenProvider {

    private final JwtTokenProvider jwtTokenProvider;

    public String createLoginToken(Long memberId, String authenticationMethod, Instant expiresAt) {
        return jwtTokenProvider.createSsoLoginToken(memberId, authenticationMethod, expiresAt);
    }

    public SsoLoginTokenClaims parseLoginToken(String rawLoginToken) {
        return jwtTokenProvider.parseSsoLoginToken(rawLoginToken);
    }
}
