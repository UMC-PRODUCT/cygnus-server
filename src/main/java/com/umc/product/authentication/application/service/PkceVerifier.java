package com.umc.product.authentication.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.umc.product.authentication.domain.PkceChallengeMethod;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;

@Component
public class PkceVerifier {

    private static final Pattern CODE_VERIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9\\-._~]{43,128}$");
    private static final Pattern CODE_CHALLENGE_PATTERN = Pattern.compile("^[A-Za-z0-9\\-._~]{43,128}$");

    public PkceChallengeMethod requireS256(String codeChallengeMethod) {
        if (!PkceChallengeMethod.S256.name().equals(codeChallengeMethod)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_PKCE);
        }
        return PkceChallengeMethod.S256;
    }

    public void requireCodeChallenge(String codeChallenge) {
        if (codeChallenge == null || !CODE_CHALLENGE_PATTERN.matcher(codeChallenge).matches()) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_PKCE);
        }
    }

    public void verify(String codeVerifier, String expectedChallenge) {
        if (codeVerifier == null
            || !CODE_VERIFIER_PATTERN.matcher(codeVerifier).matches()
            || expectedChallenge == null
            || expectedChallenge.isBlank()) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_PKCE);
        }

        String actualChallenge = calculateS256Challenge(codeVerifier);
        if (!actualChallenge.equals(expectedChallenge)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_PKCE);
        }
    }

    private String calculateS256Challenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
