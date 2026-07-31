package com.umc.product.challenger.application.port.in.query.dto;

import java.util.List;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

/**
 * 일정 생성을 위한 챌린저 정보 조회용 DTO 입니다.
 * <p>
 *
 * @param challengerId 챌린저 ID
 * @param memberId     회원 ID
 * @param gisuId       기수 정보
 * @param part         챌린저 파트
 * @param status       챌린저 활동 상태
 */
@Builder
public record ChallengerInfoWithStatus(
    Long challengerId,
    Long memberId,
    Long gisuId,
    ChallengerPart part,
    List<ChallengerTrack> tracks,
    ChallengerStatus status
) {

    public static ChallengerInfoWithStatus from(Challenger challenger) {
        return ChallengerInfoWithStatus.builder()
            .challengerId(challenger.getId())
            .memberId(challenger.getMemberId())
            .gisuId(challenger.getGisuId())
            .part(challenger.getPart())
            .tracks(challenger.getEffectiveTracks())
            .status(challenger.getStatus())
            .build();
    }
}
