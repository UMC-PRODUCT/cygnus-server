package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResultInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원 결과 조회 응답")
public record RecruitingApplicationResultResponse(
    @Schema(description = "지원서 ID", example = "100")
    Long applicationId,
    @Schema(description = "지원자에게 안내되는 고유 지원서 번호", example = "REC-2026-0001")
    String applicationNo,
    @Schema(description = "마스킹된 지원자 이메일", example = "h***@example.com")
    String maskedEmail,
    @Schema(description = "지원 track", example = "WEB_PRODUCT_ENGINEER")
    ChallengerTrack track,
    @Schema(description = "지원서 합불 및 처리 상태", example = "FINAL_PASSED")
    RecruitingApplicationStatus status,
    @Schema(description = "챌린저 등록 확정 상태", example = "CONFIRMED")
    RecruitingApplicationRegistrationStatus registrationStatus
) {

    public static RecruitingApplicationResultResponse from(RecruitingApplicationResultInfo info) {
        return new RecruitingApplicationResultResponse(
            info.applicationId(),
            info.applicationNo(),
            info.maskedEmail(),
            info.track(),
            info.status(),
            info.registrationStatus()
        );
    }
}
