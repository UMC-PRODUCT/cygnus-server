package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.domain.enums.DemodayVoteStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayAdminVoteResponse(
    @Schema(description = "표 ID", example = "9001")
    Long voteId,
    @Schema(description = "투표 시각", example = "2026-08-14T04:54:00Z")
    Instant votedAt,
    @Schema(description = "투표 참여자")
    ParticipantResponse participant,
    @Schema(description = "선택한 부스")
    DemodayBoothResponse booth,
    @Schema(description = "표 상태", example = "VALID")
    DemodayVoteStatus status,
    @Schema(description = "무효 처리 시각. 유효한 표는 null입니다.", nullable = true)
    Instant revokedAt
) {

    public static DemodayAdminVoteResponse from(DemodayAdminVoteInfo info) {
        return new DemodayAdminVoteResponse(
            info.voteId(),
            info.votedAt(),
            ParticipantResponse.from(info.participant()),
            DemodayBoothResponse.from(info.booth()),
            info.status(),
            info.revokedAt()
        );
    }

    public record ParticipantResponse(
        @Schema(description = "참여자 유형", example = "MEMBER")
        DemodayParticipantType type,
        @Schema(description = "회원명 또는 Poll 전체 기준 외부인 입장 순번", example = "이재원")
        String displayName
    ) {

        public static ParticipantResponse from(DemodayAdminVoteInfo.ParticipantInfo info) {
            return new ParticipantResponse(info.type(), info.displayName());
        }
    }
}
