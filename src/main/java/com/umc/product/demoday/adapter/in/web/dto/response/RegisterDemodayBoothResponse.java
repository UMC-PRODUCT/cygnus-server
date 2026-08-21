package com.umc.product.demoday.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "데모데이 부스 등록 결과")
public record RegisterDemodayBoothResponse(
    @Schema(description = "등록된 부스 ID", example = "20")
    Long boothId
) {

    public static RegisterDemodayBoothResponse from(Long boothId) {
        return new RegisterDemodayBoothResponse(boothId);
    }
}
