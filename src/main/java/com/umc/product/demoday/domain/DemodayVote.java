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
@Table(name = "demoday_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemodayVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demoday_poll_id", nullable = false)
    private Long pollId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "entry_code_id")
    private Long entryCodeId;

    @Column(name = "target_booth_id", nullable = false)
    private Long targetBoothId;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayVote(Long pollId, Long memberId, Long entryCodeId, Long targetBoothId) {
        this.pollId = pollId;
        this.memberId = memberId;
        this.entryCodeId = entryCodeId;
        this.targetBoothId = targetBoothId;
    }

    /**
     * 부스와 입장 코드는 식별자가 아니라 엔티티로 받는다. 표를 만드는 시점에 이들이 같은 투표에
     * 속하는지 대조해야 하는데, 식별자만 받으면 그 대조를 호출자가 대신 해야 하기 때문이다.
     */
    public static DemodayVote forMember(Long pollId, Long memberId, DemodayBooth targetBooth) {
        Objects.requireNonNull(pollId, "pollId must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(targetBooth, "targetBooth must not be null");
        requireSamePoll(pollId, targetBooth.getPollId());
        targetBooth.validateVoteTarget();

        return DemodayVote.builder()
            .pollId(pollId)
            .memberId(memberId)
            .targetBoothId(requireBoothId(targetBooth))
            .build();
    }

    public static DemodayVote forVisitor(Long pollId, DemodayEntryCode entryCode, DemodayBooth targetBooth) {
        Objects.requireNonNull(pollId, "pollId must not be null");
        Objects.requireNonNull(entryCode, "entryCode must not be null");
        Objects.requireNonNull(targetBooth, "targetBooth must not be null");
        requireSamePoll(pollId, targetBooth.getPollId());
        requireSamePoll(pollId, entryCode.getPollId());
        targetBooth.validateVoteTarget();

        return DemodayVote.builder()
            .pollId(pollId)
            .entryCodeId(requireEntryCodeId(entryCode))
            .targetBoothId(requireBoothId(targetBooth))
            .build();
    }

    private static void requireSamePoll(Long pollId, Long other) {
        if (!pollId.equals(other)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH);
        }
    }

    private static Long requireBoothId(DemodayBooth targetBooth) {
        return Objects.requireNonNull(targetBooth.getId(), "targetBooth must be persisted before voting");
    }

    private static Long requireEntryCodeId(DemodayEntryCode entryCode) {
        return Objects.requireNonNull(entryCode.getId(), "entryCode must be persisted before voting");
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (isRevoked()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_ALREADY_REVOKED);
        }

        this.revokedAt = now;
    }

    public void restore() {
        if (!isRevoked()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_NOT_REVOKED);
        }

        this.revokedAt = null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
