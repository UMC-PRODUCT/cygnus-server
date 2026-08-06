package com.umc.product.challenger.application.port.in.query.dto;

import java.util.List;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

/**
 * 챌린저 정보를 담고 있는 Info단 DTO 입니다.
 * <p>
 * 각 챌린저의 상벌점 현황을 포함하여 번환하며, 성능 상 해당 정보를 제외하고자 하는 경우 별도의 DTO를 생성해서 사용해주세요.
 */
@Builder
public record ChallengerInfo(
    Long challengerId,
    Long memberId,
    Long gisuId,
    ChallengerPart part,
    List<ChallengerTrack> tracks,
    List<ChallengerPointInfo> challengerPoints,
    Double totalPoints,
    ChallengerStatus challengerStatus
) {
    // 성능 상 상벌점 정보가 필요하지 않은 경우 별도의 DTO를 생성할 것.
    // 아직 만들지 않았어요.
    @Deprecated(since = "v1.5.0", forRemoval = true)
    public static ChallengerInfo from(Challenger challenger) {
        return ChallengerInfo.builder()
            .challengerId(challenger.getId())
            .memberId(challenger.getMemberId())
            .gisuId(challenger.getGisuId())
            .part(challenger.getPart())
            .tracks(challenger.getEffectiveTracks())
            .build();
    }

    public static ChallengerInfo from(Challenger challenger, List<ChallengerPointInfo> challengerPoints) {
        double totalPoints = challengerPoints.stream()
            .mapToDouble(ChallengerPointInfo::point)
            .sum();

        return ChallengerInfo.builder()
            .challengerId(challenger.getId())
            .memberId(challenger.getMemberId())
            .gisuId(challenger.getGisuId())
            .part(challenger.getPart())
            .tracks(challenger.getEffectiveTracks())
            .challengerPoints(challengerPoints)
            .totalPoints(totalPoints)
            .challengerStatus(challenger.getStatus())
            .build();
    }
}
