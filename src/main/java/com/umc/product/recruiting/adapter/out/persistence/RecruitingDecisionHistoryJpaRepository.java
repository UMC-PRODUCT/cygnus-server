package com.umc.product.recruiting.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingDecisionHistory;

public interface RecruitingDecisionHistoryJpaRepository extends JpaRepository<RecruitingDecisionHistory, Long> {
}
