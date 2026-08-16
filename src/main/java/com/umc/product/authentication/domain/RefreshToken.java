package com.umc.product.authentication.domain;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "uuid")
    private UUID jti;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Builder(access = AccessLevel.PRIVATE)
    private RefreshToken(UUID jti, Long memberId, Instant expiresAt, String clientId) {
        this.jti = jti;
        this.memberId = memberId;
        this.expiresAt = expiresAt;
        this.clientId = clientId;
    }

    public static RefreshToken create(UUID jti, Long memberId, Instant expiresAt) {
        return create(jti, memberId, expiresAt, null);
    }

    public static RefreshToken create(UUID jti, Long memberId, Instant expiresAt, String clientId) {
        return RefreshToken.builder()
            .jti(jti)
            .memberId(memberId)
            .expiresAt(expiresAt)
            .clientId(clientId)
            .build();
    }

    public void validateActiveFor(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!expiresAt.isAfter(Instant.now())) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        }
    }

    public void validateActiveFor(Long memberId, String clientId) {
        validateActiveFor(memberId);
        if (!normalize(this.clientId).equals(normalize(clientId))) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
