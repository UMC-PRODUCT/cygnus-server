package com.umc.product.demoday.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayPollInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayPollListResponse(
        @Schema(description = "투표 목록")
        List<DemodayPollResponse> polls
) {

    public static DemodayPollListResponse from(List<DemodayPollInfo> infos) {
        return new DemodayPollListResponse(infos.stream().map(DemodayPollResponse::from).toList());
    }
}
