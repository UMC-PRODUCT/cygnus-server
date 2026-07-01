package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationResponse(
    Long applicationId,
    String applicationNo,
    RecruitingApplicationStatus status
) {

    public static RecruitingApplicationResponse from(RecruitingApplicationInfo info) {
        return new RecruitingApplicationResponse(
            info.applicationId(),
            info.applicationNo(),
            info.status()
        );
    }
}
