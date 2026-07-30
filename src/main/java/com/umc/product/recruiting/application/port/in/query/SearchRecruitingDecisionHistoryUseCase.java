package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryPageInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;

/**
 * 중앙 운영진이 교내 회장단의 서류/최종 판정 이력을 감사 목적으로 조회합니다.
 * SUPER_ADMIN, 중앙 총괄단, 중앙 운영국·교육국만 접근할 수 있습니다.
 */
public interface SearchRecruitingDecisionHistoryUseCase {

    RecruitingDecisionHistoryPageInfo search(RecruitingDecisionHistorySearchQuery query);
}
