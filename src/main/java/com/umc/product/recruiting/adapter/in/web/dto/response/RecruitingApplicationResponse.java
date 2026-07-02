package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원서 기본 응답")
public record RecruitingApplicationResponse(
    @Schema(description = "지원서 ID", example = "100")
    Long applicationId,
    @Schema(description = "지원자에게 안내되는 고유 지원서 번호", example = "REC-2026-0001")
    String applicationNo,
    @Schema(description = "지원서 상태", example = "DRAFT")
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
