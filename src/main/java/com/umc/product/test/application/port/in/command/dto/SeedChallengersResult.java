package com.umc.product.test.application.port.in.command.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

/**
 * 챌린저 분포 시딩 결과. ADR-017 참조.
 *
 * @param gisuId         시딩 대상 기수
 * @param totalCreated   생성된 챌린저 합계
 * @param totalFailed    실패 합계 (memberFailed + challengerFailed)
 * @param perCellSummary (Chapter, School, Part 또는 Track) 셀별 결과
 */
public record SeedChallengersResult(
    Long gisuId,
    int totalCreated,
    int totalFailed,
    List<PerCellSummary> perCellSummary
) {

    /**
     * (Chapter, School, Part 또는 Track) 셀별 생성 결과.
     * <p>
     * 실패는 단계별로 분리해 응답한다.
     *
     * @param memberFailed     멤버 생성 단계에서 실패한 수
     * @param challengerFailed 챌린저 생성 단계(bulk)에서 실패한 수
     */
    public record PerCellSummary(
        Long chapterId,
        Long schoolId,
        ChallengerPart part,
        int created,
        int memberFailed,
        int challengerFailed,
        ChallengerTrack track
    ) {

        public int totalFailed() {
            return memberFailed + challengerFailed;
        }
    }
}
