package com.umc.product.demoday.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteListInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayAdminVoteListResponse(
    @Schema(description = "Poll ID", example = "101")
    Long pollId,
    @Schema(description = "voteId 내림차순 투표 기록. 무효 처리된 표도 포함됩니다.")
    List<DemodayAdminVoteResponse> content,
    @Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null입니다.", nullable = true, example = "9000")
    Long nextCursor,
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {

    public static DemodayAdminVoteListResponse from(DemodayAdminVoteListInfo info) {
        return new DemodayAdminVoteListResponse(
            info.pollId(),
            info.content().stream().map(DemodayAdminVoteResponse::from).toList(),
            info.nextCursor(),
            info.hasNext()
        );
    }
}
