package com.umc.product.demoday.adapter.in.web.dto.response;

import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayBoothResponse(
        @Schema(description = "부스 ID", example = "1")
        Long boothId,
        @Schema(description = "행사에서 사용하는 부스 코드 번호", example = "11")
        Integer boothCode,
        @Schema(description = "프로젝트 ID", example = "101", nullable = true)
        Long projectId,
        @Schema(description = "부스 표시 이름", example = "PRODUCT 팀", nullable = true)
        String displayName
) {

    public static DemodayBoothResponse from(DemodayBoothInfo info) {
        return new DemodayBoothResponse(info.boothId(), info.boothCode(), info.projectId(), info.displayName());
    }
}
