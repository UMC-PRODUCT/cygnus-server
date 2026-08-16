package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationCreatedInfo(
    Long applicationId,
    String applicationKey,
    RecruitingApplicationStatus status
) {

    public static RecruitingApplicationCreatedInfo of(
        Long applicationId,
        String applicationKey,
        RecruitingApplicationStatus status
    ) {
        return new RecruitingApplicationCreatedInfo(applicationId, applicationKey, status);
    }
}
