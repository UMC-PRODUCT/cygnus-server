package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingEvaluationTemplatePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingEvaluationTemplatePort;
import com.umc.product.recruiting.domain.RecruitingEvaluationCriterion;
import com.umc.product.recruiting.domain.RecruitingEvaluationTemplate;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationTemplateStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingEvaluationTemplatePersistenceAdapter
    implements LoadRecruitingEvaluationTemplatePort, SaveRecruitingEvaluationTemplatePort {

    private final RecruitingEvaluationTemplateJpaRepository recruitingEvaluationTemplateJpaRepository;
    private final RecruitingEvaluationCriterionJpaRepository recruitingEvaluationCriterionJpaRepository;

    @Override
    public Optional<RecruitingEvaluationTemplate> findTemplateById(Long id) {
        return recruitingEvaluationTemplateJpaRepository.findById(id);
    }

    @Override
    public RecruitingEvaluationTemplate getTemplateById(Long id) {
        return findTemplateById(id)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_EVALUATION_TEMPLATE_NOT_FOUND));
    }

    @Override
    public Optional<RecruitingEvaluationTemplate> findActiveTemplateByApplicationFormId(Long applicationFormId) {
        return recruitingEvaluationTemplateJpaRepository.findByApplicationForm_IdAndStatus(
            applicationFormId,
            RecruitingEvaluationTemplateStatus.ACTIVE
        );
    }

    @Override
    public List<RecruitingEvaluationCriterion> listCriteriaByTemplateId(Long templateId) {
        return recruitingEvaluationCriterionJpaRepository.findAllByTemplate_IdOrderBySortOrderAscIdAsc(templateId);
    }

    @Override
    public RecruitingEvaluationTemplate saveTemplate(RecruitingEvaluationTemplate template) {
        return recruitingEvaluationTemplateJpaRepository.save(template);
    }

    @Override
    public RecruitingEvaluationCriterion saveCriterion(RecruitingEvaluationCriterion criterion) {
        return recruitingEvaluationCriterionJpaRepository.save(criterion);
    }

    @Override
    public List<RecruitingEvaluationCriterion> saveCriteriaAll(Collection<RecruitingEvaluationCriterion> criteria) {
        return recruitingEvaluationCriterionJpaRepository.saveAll(criteria);
    }
}
