package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationCreatedGraphQlResponse(
    Long applicationId,
    String applicationKey,
    RecruitingApplicationStatus status
) {

    public static RecruitingApplicationCreatedGraphQlResponse from(RecruitingApplicationCreatedInfo info) {
        return new RecruitingApplicationCreatedGraphQlResponse(
            info.applicationId(),
            info.applicationKey(),
            info.status()
        );
    }
}
