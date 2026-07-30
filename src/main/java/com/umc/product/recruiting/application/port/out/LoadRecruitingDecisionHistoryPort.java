package com.umc.product.recruiting.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistoryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistorySearchCondition;

public interface LoadRecruitingDecisionHistoryPort {

    Page<RecruitingDecisionHistoryRow> searchRows(RecruitingDecisionHistorySearchCondition condition, Pageable pageable);
}
