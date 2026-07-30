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
@Table(name = "demoday_ballot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemodayBallot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demoday_vote_event_id", nullable = false)
    private DemodayVoteEvent voteEvent;

    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_code_id")
    private DemodayEntryCode entryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_booth_id", nullable = false)
    private DemodayBooth targetBooth;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DemodayBallot(DemodayVoteEvent voteEvent, Long memberId, DemodayEntryCode entryCode,
                          DemodayBooth targetBooth) {
        this.voteEvent = voteEvent;
        this.memberId = memberId;
        this.entryCode = entryCode;
        this.targetBooth = targetBooth;
    }

    public static DemodayBallot forMember(DemodayVoteEvent voteEvent, Long memberId, DemodayBooth targetBooth) {
        Objects.requireNonNull(voteEvent, "voteEvent must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(targetBooth, "targetBooth must not be null");
        requireSameVoteEvent(voteEvent, targetBooth.getVoteEvent());

        return DemodayBallot.builder()
            .voteEvent(voteEvent)
            .memberId(memberId)
            .targetBooth(targetBooth)
            .build();
    }

    public static DemodayBallot forVisitor(DemodayVoteEvent voteEvent, DemodayEntryCode entryCode,
                                           DemodayBooth targetBooth) {
        Objects.requireNonNull(voteEvent, "voteEvent must not be null");
        Objects.requireNonNull(entryCode, "entryCode must not be null");
        Objects.requireNonNull(targetBooth, "targetBooth must not be null");
        requireSameVoteEvent(voteEvent, targetBooth.getVoteEvent());
        requireSameVoteEvent(voteEvent, entryCode.getVoteEvent());

        return DemodayBallot.builder()
            .voteEvent(voteEvent)
            .entryCode(entryCode)
            .targetBooth(targetBooth)
            .build();
    }

    private static void requireSameVoteEvent(DemodayVoteEvent voteEvent, DemodayVoteEvent other) {
        if (voteEvent != other) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_BALLOT_VOTE_EVENT_MISMATCH);
        }
    }

    public void invalidate(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (isVoided()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_BALLOT_ALREADY_VOIDED);
        }

        this.voidedAt = now;
    }

    public boolean isVoided() {
        return voidedAt != null;
    }
}
