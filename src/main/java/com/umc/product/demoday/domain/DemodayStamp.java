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

    @Column(name = "entry_code_id")
    private Long entryCodeId;

    @Column(name = "booth_id", nullable = false)
    private Long boothId;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayStamp(Long memberId, Long entryCodeId, Long boothId) {
        this.memberId = memberId;
        this.entryCodeId = entryCodeId;
        this.boothId = boothId;
    }

    public static DemodayStamp forMember(Long memberId, DemodayBooth booth) {
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(booth, "booth must not be null");

        return DemodayStamp.builder()
            .memberId(memberId)
            .boothId(requireBoothId(booth))
            .build();
    }

    /**
     * 부스와 입장 코드는 식별자가 아니라 엔티티로 받는다. 스탬프는 poll FK를 갖지 않으므로
     * 두 엔티티가 서로 같은 투표에 속하는지 여기서 직접 대조해야 하기 때문이다.
     */
    public static DemodayStamp forVisitor(DemodayEntryCode entryCode, DemodayBooth booth) {
        Objects.requireNonNull(entryCode, "entryCode must not be null");
        Objects.requireNonNull(booth, "booth must not be null");
        requireSamePoll(entryCode.getPollId(), booth.getPollId());

        return DemodayStamp.builder()
            .entryCodeId(requireEntryCodeId(entryCode))
            .boothId(requireBoothId(booth))
            .build();
    }

    private static void requireSamePoll(Long pollId, Long other) {
        if (!pollId.equals(other)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_STAMP_POLL_MISMATCH);
        }
    }

    private static Long requireBoothId(DemodayBooth booth) {
        return Objects.requireNonNull(booth.getId(), "booth must be persisted before stamping");
    }

    private static Long requireEntryCodeId(DemodayEntryCode entryCode) {
        return Objects.requireNonNull(entryCode.getId(), "entryCode must be persisted before stamping");
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
