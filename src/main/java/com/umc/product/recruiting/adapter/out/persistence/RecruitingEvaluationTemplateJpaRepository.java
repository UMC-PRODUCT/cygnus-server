package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingEvaluationTemplate;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationTemplateStatus;

public interface RecruitingEvaluationTemplateJpaRepository extends JpaRepository<RecruitingEvaluationTemplate, Long> {

    Optional<RecruitingEvaluationTemplate> findByApplicationForm_IdAndStatus(
        Long applicationFormId,
        RecruitingEvaluationTemplateStatus status
    );
}
