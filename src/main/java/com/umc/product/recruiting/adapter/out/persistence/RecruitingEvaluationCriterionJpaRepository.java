package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingEvaluationCriterion;

public interface RecruitingEvaluationCriterionJpaRepository extends JpaRepository<RecruitingEvaluationCriterion, Long> {

    List<RecruitingEvaluationCriterion> findAllByTemplate_IdOrderBySortOrderAscIdAsc(Long templateId);
}
