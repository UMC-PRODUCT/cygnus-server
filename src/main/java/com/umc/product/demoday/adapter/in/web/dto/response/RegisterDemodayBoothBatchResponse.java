package com.umc.product.demoday.adapter.in.web.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "데모데이 부스 일괄 등록 결과")
public record RegisterDemodayBoothBatchResponse(
    @Schema(description = "등록된 부스 수", example = "3")
    int registeredCount,

    @Schema(description = "등록된 부스 ID. 요청한 부스 목록과 순서가 같습니다.", example = "[20, 21, 22]")
    List<Long> boothIds
) {

    public static RegisterDemodayBoothBatchResponse from(List<Long> boothIds) {
        return new RegisterDemodayBoothBatchResponse(boothIds.size(), boothIds);
    }
}
