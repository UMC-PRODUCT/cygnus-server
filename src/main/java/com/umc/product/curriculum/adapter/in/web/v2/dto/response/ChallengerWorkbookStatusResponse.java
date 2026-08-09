package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import com.umc.product.curriculum.domain.enums.ChallengerWorkbookStatus;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

public enum ChallengerWorkbookStatusResponse {
    PASS,
    FAIL,
    IN_PROGRESS;

    /**
     * 워크북 단건 조회 응답용 변환.
     * <p>
     * 단건 조회는 워크북이 존재할 때만 도달하므로 {@link ChallengerWorkbookStatus#NOT_SUBMITTED} 는 나올 수 없다.
     */
    public static ChallengerWorkbookStatusResponse from(ChallengerWorkbookStatus status) {
        return switch (status) {
            case PASS -> PASS;
            case FAIL -> FAIL;
            case IN_PROGRESS -> IN_PROGRESS;
            case NOT_SUBMITTED -> throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_NOT_FOUND);
        };
    }
}
