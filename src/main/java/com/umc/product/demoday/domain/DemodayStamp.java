package com.umc.product.demoday.domain;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "demoday_stamp")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemodayStamp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_code_id")
    private DemodayEntryCode entryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private DemodayBooth booth;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayStamp(Long memberId, DemodayEntryCode entryCode, DemodayBooth booth) {
        this.memberId = memberId;
        this.entryCode = entryCode;
        this.booth = booth;
    }

    public static DemodayStamp forMember(Long memberId, DemodayBooth booth) {
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(booth, "booth must not be null");

        return DemodayStamp.builder()
            .memberId(memberId)
            .booth(booth)
            .build();
    }

    public static DemodayStamp forVisitor(DemodayEntryCode entryCode, DemodayBooth booth) {
        Objects.requireNonNull(entryCode, "entryCode must not be null");
        Objects.requireNonNull(booth, "booth must not be null");
        requireSamePoll(entryCode.getPoll(), booth.getPoll());

        return DemodayStamp.builder()
            .entryCode(entryCode)
            .booth(booth)
            .build();
    }

    /**
     * 스탬프는 poll FK를 갖지 않으므로 입장 코드와 부스가 서로 같은 투표에 속하는지 직접 대조한다.
     */
    private static void requireSamePoll(DemodayPoll poll, DemodayPoll other) {
        if (!isSamePoll(poll, other)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_STAMP_POLL_MISMATCH);
        }
    }

    private static boolean isSamePoll(DemodayPoll left, DemodayPoll right) {
        if (left == right) {
            return true;
        }

        Long leftId = left.getId();
        return leftId != null && leftId.equals(right.getId());
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (isRevoked()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_STAMP_ALREADY_REVOKED);
        }

        this.revokedAt = now;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
