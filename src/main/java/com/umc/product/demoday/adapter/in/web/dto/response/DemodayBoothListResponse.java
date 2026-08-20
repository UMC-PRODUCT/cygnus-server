package com.umc.product.demoday.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayBoothListResponse(
        @Schema(description = "투표 가능한 프로젝트 부스 목록. 스탬프 적립 전용 외부 부스는 포함하지 않습니다.")
        List<DemodayBoothResponse> booths
) {

    public static DemodayBoothListResponse from(List<DemodayBoothInfo> infos) {
        return new DemodayBoothListResponse(infos.stream().map(DemodayBoothResponse::from).toList());
    }
}
