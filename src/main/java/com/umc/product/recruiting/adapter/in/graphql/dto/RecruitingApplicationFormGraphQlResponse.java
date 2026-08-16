package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingApplicationFormGraphQlResponse(
    Long applicationFormId,
    Long roundId,
    RecruitingRoundType roundType,
    Integer roundNo,
    Long formId,
    RecruitingApplicationFormStatus status
) {

    public static RecruitingApplicationFormGraphQlResponse from(RecruitingApplicationFormInfo info) {
        return new RecruitingApplicationFormGraphQlResponse(
            info.applicationFormId(),
            info.roundId(),
            info.roundType(),
            info.roundNo(),
            info.formId(),
            info.status()
        );
    }
}
