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
@Table(name = "demoday_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemodayVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demoday_poll_id", nullable = false)
    private DemodayPoll poll;

    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_code_id")
    private DemodayEntryCode entryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_booth_id", nullable = false)
    private DemodayBooth targetBooth;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayVote(DemodayPoll poll, Long memberId, DemodayEntryCode entryCode,
                          DemodayBooth targetBooth) {
        this.poll = poll;
        this.memberId = memberId;
        this.entryCode = entryCode;
        this.targetBooth = targetBooth;
    }

    public static DemodayVote forMember(DemodayPoll poll, Long memberId, DemodayBooth targetBooth) {
        Objects.requireNonNull(poll, "poll must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(targetBooth, "targetBooth must not be null");
        requireSamePoll(poll, targetBooth.getPoll());

        return DemodayVote.builder()
            .poll(poll)
            .memberId(memberId)
            .targetBooth(targetBooth)
            .build();
    }

    public static DemodayVote forVisitor(DemodayPoll poll, DemodayEntryCode entryCode,
                                           DemodayBooth targetBooth) {
        Objects.requireNonNull(poll, "poll must not be null");
        Objects.requireNonNull(entryCode, "entryCode must not be null");
        Objects.requireNonNull(targetBooth, "targetBooth must not be null");
        requireSamePoll(poll, targetBooth.getPoll());
        requireSamePoll(poll, entryCode.getPoll());

        return DemodayVote.builder()
            .poll(poll)
            .entryCode(entryCode)
            .targetBooth(targetBooth)
            .build();
    }

    private static void requireSamePoll(DemodayPoll poll, DemodayPoll other) {
        if (!isSamePoll(poll, other)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH);
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
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_ALREADY_REVOKED);
        }

        this.revokedAt = now;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
