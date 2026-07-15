package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "recruiting_round_evaluator",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_round_evaluator_round_member",
        columnNames = {"recruiting_round_id", "member_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingRoundEvaluator extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_round_id", nullable = false)
    private RecruitingRound round;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingRoundEvaluator(RecruitingRound round, Long memberId) {
        validate(round, memberId);
        this.round = round;
        this.memberId = memberId;
    }

    public static RecruitingRoundEvaluator create(RecruitingRound round, Long memberId) {
        return RecruitingRoundEvaluator.builder()
            .round(round)
            .memberId(memberId)
            .build();
    }

    private static void validate(RecruitingRound round, Long memberId) {
        if (round == null || memberId == null || memberId <= 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_EVALUATOR_INVALID);
        }
    }
}
