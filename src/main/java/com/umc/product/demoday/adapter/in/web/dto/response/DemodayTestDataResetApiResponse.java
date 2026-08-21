package com.umc.product.demoday.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 성공 응답으로 감싼 데모데이 테스트 데이터 초기화 결과")
public record DemodayTestDataResetApiResponse(
    @Schema(description = "요청 성공 여부", example = "true")
    boolean success,

    @Schema(description = "응답 코드", example = "COMMON200")
    String code,

    @Schema(description = "응답 메시지", example = "성공입니다.")
    String message,

    @Schema(description = "데모데이 테스트 데이터 초기화 결과")
    DemodayTestDataResetResponse result
) {
}
