package com.umc.product.demoday.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "데모데이 투표 생성 결과")
public record CreateDemodayPollResponse(
    @Schema(description = "생성된 데모데이 투표 ID", example = "1")
    Long pollId
) {

    public static CreateDemodayPollResponse from(Long pollId) {
        return new CreateDemodayPollResponse(pollId);
    }
}
