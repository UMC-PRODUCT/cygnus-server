package com.umc.product.recruiting.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationTemplateStatus;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recruiting_evaluation_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitingEvaluationTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiting_application_form_id", nullable = false)
    private RecruitingApplicationForm applicationForm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitingEvaluationTemplateStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private RecruitingEvaluationTemplate(RecruitingApplicationForm applicationForm) {
        this.applicationForm = applicationForm;
        this.status = RecruitingEvaluationTemplateStatus.ACTIVE;
    }

    public static RecruitingEvaluationTemplate createDefault(RecruitingApplicationForm applicationForm) {
        return RecruitingEvaluationTemplate.builder()
            .applicationForm(applicationForm)
            .build();
    }

    public void archive() {
        this.status = RecruitingEvaluationTemplateStatus.ARCHIVED;
    }
}
