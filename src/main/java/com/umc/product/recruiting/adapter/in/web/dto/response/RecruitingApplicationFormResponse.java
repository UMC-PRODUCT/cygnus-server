package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리크루팅 지원 폼 응답")
public record RecruitingApplicationFormResponse(
    @Schema(description = "리크루팅 도메인의 지원 폼 연결 ID", example = "30")
    Long applicationFormId,
    @Schema(description = "모집 차수 ID", example = "20")
    Long roundId,
    @Schema(description = "모집 차수 유형", example = "REGULAR")
    RecruitingRoundType roundType,
    @Schema(description = "추가모집 차수 번호", example = "1")
    Integer roundNo,
    @Schema(description = "form 엔진의 폼 ID", example = "500")
    Long formId,
    @Schema(description = "지원 폼 게시 상태", example = "PUBLISHED")
    RecruitingApplicationFormStatus status
) {

    public static RecruitingApplicationFormResponse from(RecruitingApplicationFormInfo info) {
        return new RecruitingApplicationFormResponse(
            info.applicationFormId(),
            info.roundId(),
            info.roundType(),
            info.roundNo(),
            info.formId(),
            info.status()
        );
    }
}
