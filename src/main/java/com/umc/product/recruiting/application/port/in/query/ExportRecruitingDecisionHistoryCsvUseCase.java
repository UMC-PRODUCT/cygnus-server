package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;

/**
 * 평가 이력을 CSV로 내려받습니다. 082 CSV와 같은 원칙으로 원문 email과 실명은 포함하지 않습니다.
 */
public interface ExportRecruitingDecisionHistoryCsvUseCase {

    byte[] exportCsv(RecruitingDecisionHistorySearchQuery query);
}
