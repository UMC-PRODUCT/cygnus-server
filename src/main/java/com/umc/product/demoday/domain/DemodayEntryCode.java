package com.umc.product.demoday.domain;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "demoday_entry_code",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_demoday_entry_code_hash",
        columnNames = "code_hash"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemodayEntryCode extends BaseEntity {

    public static final int HASH_LENGTH = 64;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 투표를 식별자로만 참조한다.
     */
    @Column(name = "demoday_poll_id", nullable = false)
    private Long pollId;

    @Column(name = "code_hash", nullable = false, length = HASH_LENGTH)
    private String codeHash;

    /**
     * 소셜 identity 바인딩은 시간 남으면 진행하는 것으로 결정됨
     * DB의 uk_demoday_entry_code_identity는 partial unique(WHERE ... IS NOT NULL)라
     * NULL인 동안 제약이 없고 값을 채우기 시작하면 마이그레이션 없이 제약을 사용할 수 있다.
     */
    @Column(name = "bound_identity_hash", length = HASH_LENGTH)
    private String boundIdentityHash;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "redemption_request_id_hash", length = HASH_LENGTH)
    private String redemptionRequestIdHash;

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayEntryCode(Long pollId, String codeHash) {
        this.pollId = pollId;
        this.codeHash = codeHash;
    }

    public static DemodayEntryCode create(Long pollId, String codeHash) {
        Objects.requireNonNull(pollId, "pollId must not be null");
        Objects.requireNonNull(codeHash, "codeHash must not be null");
        return DemodayEntryCode.builder()
            .pollId(pollId)
            .codeHash(codeHash)
            .build();
    }

    public void redeem(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (isRedeemed()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_ALREADY_REDEEMED);
        }

        this.redeemedAt = now;
    }

    public boolean redeemOrResume(Instant now, String requestIdHash) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(requestIdHash, "requestIdHash must not be null");

        if (!isRedeemed()) {
            this.redeemedAt = now;
            this.redemptionRequestIdHash = requestIdHash;
            return true;
        }

        if (requestIdHash.equals(redemptionRequestIdHash)) {
            return false;
        }

        throw new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_ALREADY_REDEEMED);
    }

    public boolean isRedeemed() {
        return redeemedAt != null;
    }
}
