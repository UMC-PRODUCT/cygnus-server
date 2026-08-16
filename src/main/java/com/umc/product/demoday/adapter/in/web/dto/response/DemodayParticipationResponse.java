package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayParticipationResponse(
        @Schema(description = "투표 ID", example = "1")
        Long pollId,
        @Schema(description = "참여자 유형", example = "MEMBER")
        DemodayParticipantType participantType,
        @Schema(description = "적립한 스탬프 수", example = "4")
        int stampCount,
        @Schema(description = "투표 페이지 진입에 필요한 스탬프 수", example = "6")
        int requiredStampCount,
        @Schema(description = "적립한 스탬프 목록")
        List<DemodayStampResponse> stamps,
        @Schema(description = "다음 스탬프 적립 가능 시각", nullable = true)
        Instant nextStampAvailableAt,
        @Schema(description = "투표 완료 여부", example = "false")
        boolean hasVoted,
        @Schema(description = "투표 페이지 진입 가능 여부", example = "false")
        boolean canEnterVotePage
) {

    public static DemodayParticipationResponse from(DemodayParticipationInfo info) {
        return new DemodayParticipationResponse(
                info.pollId(),
                info.participantType(),
                info.stampCount(),
                info.requiredStampCount(),
                info.stamps().stream().map(DemodayStampResponse::from).toList(),
                info.nextStampAvailableAt(),
                info.hasVoted(),
                info.canEnterVotePage()
        );
    }
}
