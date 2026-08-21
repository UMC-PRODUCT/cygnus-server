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
        @Schema(description = "현재 집계에 포함되는 유효한 표가 있는지", example = "false")
        boolean hasActiveVote,
        @Schema(description = "취소 여부와 무관하게 이번 Poll의 투표 기회를 이미 사용했는지", example = "false")
        boolean hasUsedVoteSlot,
        @Schema(description = "스탬프 조건을 충족하고 투표 기회를 사용하지 않아 INFO QR 인증을 요청할 수 있는지", example = "false")
        boolean canRequestVoteAuthorization,
        @Schema(description = "현재 집계에 포함되는 유효한 표의 영수증", nullable = true)
        DemodayVoteReceiptResponse activeVoteReceipt
) {

    public static DemodayParticipationResponse from(DemodayParticipationInfo info) {
        return new DemodayParticipationResponse(
                info.pollId(),
                info.participantType(),
                info.stampCount(),
                info.requiredStampCount(),
                info.stamps().stream().map(DemodayStampResponse::from).toList(),
                info.nextStampAvailableAt(),
                info.hasActiveVote(),
                info.hasUsedVoteSlot(),
                info.canRequestVoteAuthorization(),
                info.activeVoteReceipt() == null ? null : DemodayVoteReceiptResponse.from(info.activeVoteReceipt())
        );
    }
}
