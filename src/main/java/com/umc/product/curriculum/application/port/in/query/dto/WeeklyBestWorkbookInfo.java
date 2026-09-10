package com.umc.product.curriculum.application.port.in.query.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

import lombok.Builder;

/**
 * 베스트 워크북 조회 결과 Info
 *
 * @param weeklyBestWorkbookEntityId WeeklyBestWorkbook Entity PK
 * @param challengerId               베스트 워크북으로 선정받은 챌린저의 멤버 ID
 * @param gisuId                     베스트 워크북으로 선정받은 챌린저의 기수 ID
 * @param part                       베스트 워크북으로 선정받은 챌린저의 파트
 * @param studyGroupId               소속 스터디 그룹 ID
 * @param decidedMemberId            베스트 워크북을 선정한 운영진의 멤버 ID
 * @param reason                     선정 사유
 * @param challengerWorkbooks        해당 주차의 챌린저 워크북 목록
 */
@Builder
public record WeeklyBestWorkbookInfo(
    Long weeklyBestWorkbookEntityId,
    Long challengerId,
    Long gisuId,
    ChallengerPart part,
    ChallengerTrack track,
    Long studyGroupId,
    Long decidedMemberId,
    String reason,
    List<ChallengerWorkbookInfo> challengerWorkbooks
) {

    public WeeklyBestWorkbookInfo(
        Long weeklyBestWorkbookEntityId, Long challengerId, Long gisuId, ChallengerPart part,
        Long studyGroupId, Long decidedMemberId, String reason, List<ChallengerWorkbookInfo> challengerWorkbooks
    ) {
        this(weeklyBestWorkbookEntityId, challengerId, gisuId, part, null,
            studyGroupId, decidedMemberId, reason, challengerWorkbooks);
    }
}
