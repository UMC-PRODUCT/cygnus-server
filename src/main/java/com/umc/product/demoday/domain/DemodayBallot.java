package com.umc.product.demoday.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;

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
}
