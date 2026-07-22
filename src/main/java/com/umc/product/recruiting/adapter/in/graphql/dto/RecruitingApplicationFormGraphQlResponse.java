package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;

public record RecruitingApplicationFormGraphQlResponse(
    Long id,
    Long roundId,
    Long formId,
    RecruitingApplicationFormStatus status
) {

    public static RecruitingApplicationFormGraphQlResponse from(RecruitingApplicationFormInfo info) {
        return new RecruitingApplicationFormGraphQlResponse(
            info.applicationFormId(),
            info.roundId(),
            info.formId(),
            info.status()
        );
    }
}
