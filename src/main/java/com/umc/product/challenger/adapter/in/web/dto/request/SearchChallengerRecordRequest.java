package com.umc.product.challenger.adapter.in.web.dto.request;

import org.springframework.data.domain.Pageable;

import com.umc.product.challenger.application.port.in.query.dto.ListChallengerRecordsQuery;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

/**
 * 조건별 챌린저 기록 코드 목록 조회 요청 (CHALLENGER-RECORD-103).
 * <p>
 * 모든 조건은 선택이며 조합 가능합니다.
 */
public record SearchChallengerRecordRequest(
    Long gisuId,
    Long schoolId,
    ChallengerPart part,
    ChallengerRoleType challengerRoleType
) {
    public ListChallengerRecordsQuery toQuery(Pageable pageable) {
        return ListChallengerRecordsQuery.builder()
            .gisuId(gisuId)
            .schoolId(schoolId)
            .part(part)
            .challengerRoleType(challengerRoleType)
            .pageable(pageable)
            .build();
    }
}
