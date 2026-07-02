package com.umc.product.challenger.adapter.in.web.dto.response;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import java.time.Instant;
import lombok.Builder;

/**
 * 조건별 챌린저 기록 코드 목록 조회 응답 항목 (CHALLENGER-RECORD-103).
 * <p>
 * 단건 조회용 {@link ChallengerRecordResponse} 에 관리·삭제에 필요한 {@code id} 와
 * 사용 상태({@code isUsed}, {@code usedMemberId}, {@code usedAt})를 추가한 관리자용 응답입니다.
 */
@Builder
public record ChallengerRecordSummaryResponse(
    Long id,
    String code,
    ChallengerPart part,
    Long gisuId,
    Long gisu,
    Long schoolId,
    String schoolName,
    Long chapterId,
    String chapterName,
    String memberName,
    ChallengerRoleType challengerRoleType,
    Long organizationId,
    boolean isUsed,
    Long usedMemberId,
    Instant usedAt
) {
}
