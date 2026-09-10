package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.dto.WeeklyBestWorkbookInfo;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;

import lombok.Builder;

/**
 * 베스트 워크북에 대한 정보를 조회합니다.
 *
 * @param weeklyBestWorkbookEntityId {@link WeeklyBestWorkbook}의 PK
 * @param memberId                   베스트 워크북으로 선정받은 회원 ID
 * @param studyGroupId               베스트 워크북으로 선정받은 스터디 그룹 ID
 * @param decidedMemberId            베스트 워크북을 선정한 회원 ID
 * @param reason                     베스트 워크북 선정 사유
 * @param challengerWorkbooks        베스트 워크북으로 선정된 주차의 워크북들
 */
@Builder
public record BestWorkbookResponse(
    Long weeklyBestWorkbookEntityId,
    Long memberId,
    Long studyGroupId, // NOT NULL!
    Long decidedMemberId,
    String reason,
    List<ChallengerWorkbookResponse> challengerWorkbooks,
    ChallengerTrack track
) {
    public BestWorkbookResponse(
        Long weeklyBestWorkbookEntityId, Long memberId, Long studyGroupId, Long decidedMemberId,
        String reason, List<ChallengerWorkbookResponse> challengerWorkbooks
    ) {
        this(weeklyBestWorkbookEntityId, memberId, studyGroupId, decidedMemberId, reason, challengerWorkbooks, null);
    }

    public static BestWorkbookResponse from(WeeklyBestWorkbookInfo info) {
        return BestWorkbookResponse.builder()
            .weeklyBestWorkbookEntityId(info.weeklyBestWorkbookEntityId())
            .memberId(info.challengerId())
            .studyGroupId(info.studyGroupId())
            .decidedMemberId(info.decidedMemberId())
            .reason(info.reason())
            .track(info.track())
            .challengerWorkbooks(info.challengerWorkbooks().stream()
                .map(ChallengerWorkbookResponse::from)
                .toList())
            .build();
    }
}
