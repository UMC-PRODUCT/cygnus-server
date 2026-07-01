package com.umc.product.recruiting.domain;

import java.util.List;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "recruiting_evaluation_criterion",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_recruiting_evaluation_criterion_template_sort",
        columnNames = {"recruiting_evaluation_template_id", "sort_order"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingEvaluationCriterion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_evaluation_template_id", nullable = false)
    private RecruitingEvaluationTemplate template;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, name = "score_min")
    private Integer scoreMin;

    @Column(nullable = false, name = "score_max")
    private Integer scoreMax;

    @Column(nullable = false, name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean required;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingEvaluationCriterion(
        RecruitingEvaluationTemplate template,
        String name,
        String description,
        Integer scoreMin,
        Integer scoreMax,
        Integer sortOrder,
        Boolean required
    ) {
        this.template = template;
        this.name = name;
        this.description = description;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.sortOrder = sortOrder;
        this.required = required;
    }

    public static RecruitingEvaluationCriterion create(
        RecruitingEvaluationTemplate template,
        String name,
        String description,
        Integer scoreMin,
        Integer scoreMax,
        Integer sortOrder,
        Boolean required
    ) {
        return RecruitingEvaluationCriterion.builder()
            .template(template)
            .name(name)
            .description(description)
            .scoreMin(scoreMin)
            .scoreMax(scoreMax)
            .sortOrder(sortOrder)
            .required(required)
            .build();
    }

    public static List<RecruitingEvaluationCriterion> createDefaults(RecruitingEvaluationTemplate template) {
        return List.of(
            create(template, "역량", "지원 트랙에 필요한 역량", 1, 5, 1, true),
            create(template, "협업", "동료와 함께 일하는 태도", 1, 5, 2, true),
            create(template, "성장 가능성", "활동 기간 중 성장 가능성", 1, 5, 3, true)
        );
    }
}
