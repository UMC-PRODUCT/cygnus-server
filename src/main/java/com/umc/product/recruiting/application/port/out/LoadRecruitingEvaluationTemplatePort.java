package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingEvaluationCriterion;
import com.umc.product.recruiting.domain.RecruitingEvaluationTemplate;

public interface LoadRecruitingEvaluationTemplatePort {

    Optional<RecruitingEvaluationTemplate> findTemplateById(Long id);

    RecruitingEvaluationTemplate getTemplateById(Long id);

    Optional<RecruitingEvaluationTemplate> findActiveTemplateByApplicationFormId(Long applicationFormId);

    List<RecruitingEvaluationCriterion> listCriteriaByTemplateId(Long templateId);
}
