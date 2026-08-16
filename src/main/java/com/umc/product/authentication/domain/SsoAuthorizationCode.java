package com.umc.product.authentication.domain;

import java.time.Instant;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sso_authorization_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SsoAuthorizationCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "redirect_uri", nullable = false, length = 500)
    private String redirectUri;

    @Column(name = "code_challenge", nullable = false, length = 128)
    private String codeChallenge;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_challenge_method", nullable = false, length = 20)
    private PkceChallengeMethod codeChallengeMethod;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private SsoAuthorizationCode(
        String codeHash,
        Long memberId,
        String clientId,
        String redirectUri,
        String codeChallenge,
        PkceChallengeMethod codeChallengeMethod,
        Instant expiresAt
    ) {
        validateCreateArgs(codeHash, memberId, clientId, redirectUri, codeChallenge, codeChallengeMethod, expiresAt);
        this.codeHash = codeHash;
        this.memberId = memberId;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
        this.expiresAt = expiresAt;
    }

    public static SsoAuthorizationCode create(
        String codeHash,
        Long memberId,
        String clientId,
        String redirectUri,
        String codeChallenge,
        PkceChallengeMethod codeChallengeMethod,
        Instant expiresAt
    ) {
        return SsoAuthorizationCode.builder()
            .codeHash(codeHash)
            .memberId(memberId)
            .clientId(clientId)
            .redirectUri(redirectUri)
            .codeChallenge(codeChallenge)
            .codeChallengeMethod(codeChallengeMethod)
            .expiresAt(expiresAt)
            .build();
    }

    public void consume(String requestedClientId, String requestedRedirectUri, Instant now) {
        if (usedAt != null) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_CODE);
        }
        if (now == null || !expiresAt.isAfter(now)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_SSO_AUTHORIZATION_CODE);
        }
        if (!clientId.equals(requestedClientId) || !redirectUri.equals(requestedRedirectUri)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_CODE);
        }
        this.usedAt = now;
    }

    private static void validateCreateArgs(
        String codeHash,
        Long memberId,
        String clientId,
        String redirectUri,
        String codeChallenge,
        PkceChallengeMethod codeChallengeMethod,
        Instant expiresAt
    ) {
        if (isBlank(codeHash)
            || memberId == null
            || memberId <= 0
            || isBlank(clientId)
            || isBlank(redirectUri)
            || isBlank(codeChallenge)
            || codeChallengeMethod == null
            || expiresAt == null) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_REQUEST);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
