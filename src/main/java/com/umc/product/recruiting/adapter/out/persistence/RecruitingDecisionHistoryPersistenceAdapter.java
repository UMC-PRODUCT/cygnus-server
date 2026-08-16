package com.umc.product.recruiting.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingDecisionHistoryPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingDecisionHistoryPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistoryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistorySearchCondition;
import com.umc.product.recruiting.domain.RecruitingDecisionHistory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingDecisionHistoryPersistenceAdapter
    implements LoadRecruitingDecisionHistoryPort, SaveRecruitingDecisionHistoryPort {

    private final RecruitingDecisionHistoryJpaRepository recruitingDecisionHistoryJpaRepository;
    private final RecruitingDecisionHistoryQueryRepository recruitingDecisionHistoryQueryRepository;

    @Override
    public Page<RecruitingDecisionHistoryRow> searchRows(
        RecruitingDecisionHistorySearchCondition condition,
        Pageable pageable
    ) {
        return recruitingDecisionHistoryQueryRepository.searchRows(condition, pageable);
    }

    @Override
    public RecruitingDecisionHistory save(RecruitingDecisionHistory decisionHistory) {
        return recruitingDecisionHistoryJpaRepository.save(decisionHistory);
    }
}
