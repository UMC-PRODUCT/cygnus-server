package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "uk_recruiting_round_evaluator_round_member_stage",
        columnNames = {"recruiting_round_id", "member_id", "stage"}
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingEvaluatorStage stage;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingRoundEvaluator(RecruitingRound round, Long memberId, RecruitingEvaluatorStage stage) {
        validate(round, memberId, stage);
        this.round = round;
        this.memberId = memberId;
        this.stage = stage;
    }

    public static RecruitingRoundEvaluator create(
        RecruitingRound round,
        Long memberId,
        RecruitingEvaluatorStage stage
    ) {
        return RecruitingRoundEvaluator.builder()
            .round(round)
            .memberId(memberId)
            .stage(stage)
            .build();
    }

    private static void validate(RecruitingRound round, Long memberId, RecruitingEvaluatorStage stage) {
        if (round == null || memberId == null || memberId <= 0 || stage == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_EVALUATOR_INVALID);
        }
    }
}
