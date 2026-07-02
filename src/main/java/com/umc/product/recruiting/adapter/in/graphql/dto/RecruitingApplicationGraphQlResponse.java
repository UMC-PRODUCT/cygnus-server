package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationGraphQlResponse(
    Long applicationId,
    String applicationNo,
    RecruitingApplicationStatus status
) {

    public static RecruitingApplicationGraphQlResponse from(RecruitingApplicationInfo info) {
        return new RecruitingApplicationGraphQlResponse(
            info.applicationId(),
            info.applicationNo(),
            info.status()
        );
    }
}
