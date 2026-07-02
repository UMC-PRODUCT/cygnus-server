package com.umc.product.challenger.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import java.util.Objects;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

/**
 * 조건별 챌린저 기록 코드 목록 조회 Query (CHALLENGER-RECORD-103).
 * <p>
 * 모든 필터 조건({@code gisuId}, {@code schoolId}, {@code part}, {@code challengerRoleType})은
 * 선택이며 자유롭게 조합할 수 있습니다. null 인 조건은 필터에서 제외됩니다.
 */
@Builder
public record ListChallengerRecordsQuery(
    Long gisuId,
    Long schoolId,
    ChallengerPart part,
    ChallengerRoleType challengerRoleType,
    Pageable pageable
) {
    public ListChallengerRecordsQuery {
        Objects.requireNonNull(pageable, "pageable must not be null");
    }
}
