package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.Builder;

@Builder
public record RecruitingApplicationInfo(
    Long applicationId,
    String applicationNo,
    RecruitingApplicationStatus status
) {

    public static RecruitingApplicationInfo from(Long applicationId, String applicationNo, RecruitingApplicationStatus status) {
        return RecruitingApplicationInfo.builder()
            .applicationId(applicationId)
            .applicationNo(applicationNo)
            .status(status)
            .build();
    }
}
