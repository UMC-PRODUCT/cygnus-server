package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원서 초안 생성 응답")
public record RecruitingApplicationCreatedResponse(
    @Schema(description = "지원서 ID", example = "100") Long applicationId,
    @Schema(description = "지원서 생성 시 한 번 반환되는 application key", example = "A1B2C3") String applicationKey,
    @Schema(description = "지원서 상태", example = "DRAFT") RecruitingApplicationStatus status
) {

    public static RecruitingApplicationCreatedResponse from(RecruitingApplicationCreatedInfo info) {
        return new RecruitingApplicationCreatedResponse(
            info.applicationId(),
            info.applicationKey(),
            info.status()
        );
    }
}
