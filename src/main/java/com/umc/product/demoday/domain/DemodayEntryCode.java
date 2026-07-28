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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demoday_vote_event_id", nullable = false)
    private DemodayVoteEvent voteEvent;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "bound_identity_hash")
    private String boundIdentityHash;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;
}
