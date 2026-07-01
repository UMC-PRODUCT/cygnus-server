package com.umc.product.recruiting.application.port.out;

import java.util.Collection;
import java.util.List;

import com.umc.product.recruiting.domain.RecruitingEvaluationCriterion;
import com.umc.product.recruiting.domain.RecruitingEvaluationTemplate;

public interface SaveRecruitingEvaluationTemplatePort {

    RecruitingEvaluationTemplate saveTemplate(RecruitingEvaluationTemplate template);

    RecruitingEvaluationCriterion saveCriterion(RecruitingEvaluationCriterion criterion);

    List<RecruitingEvaluationCriterion> saveCriteriaAll(Collection<RecruitingEvaluationCriterion> criteria);
}
