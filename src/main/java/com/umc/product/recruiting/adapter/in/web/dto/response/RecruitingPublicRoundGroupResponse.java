package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundGroupInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시즌별 공개 모집 목록")
public record RecruitingPublicRoundGroupResponse(
    Long seasonId,
    Long gisuId,
    Long chapterId,
    String chapterName,
    Long schoolId,
    String schoolName,
    List<RoundResponse> rounds
) {

    public static RecruitingPublicRoundGroupResponse from(RecruitingPublicRoundGroupInfo info) {
        return new RecruitingPublicRoundGroupResponse(
            info.seasonId(),
            info.gisuId(),
            info.chapterId(),
            info.chapterName(),
            info.schoolId(),
            info.schoolName(),
            info.rounds().stream().map(RoundResponse::from).toList()
        );
    }

    public record RoundResponse(
        Long roundId,
        String title,
        RecruitingRoundType type,
        Integer roundNo,
        List<ChallengerTrack> recruitableTracks,
        boolean secondChoiceEnabled,
        Instant documentStartAt,
        Instant documentEndAt,
        Instant documentResultPublishedAt,
        boolean interviewRequired,
        Instant interviewStartAt,
        Instant interviewEndAt,
        Instant finalResultPublishedAt,
        String announcement,
        Long applicationFormId,
        Long formId,
        boolean applicationOpen
    ) {

        private static RoundResponse from(RecruitingPublicRoundInfo info) {
            return new RoundResponse(
                info.roundId(),
                info.title(),
                info.type(),
                info.roundNo(),
                info.recruitableTracks(),
                info.secondChoiceEnabled(),
                info.documentStartAt(),
                info.documentEndAt(),
                info.documentResultPublishedAt(),
                info.interviewRequired(),
                info.interviewStartAt(),
                info.interviewEndAt(),
                info.finalResultPublishedAt(),
                info.announcement(),
                info.applicationFormId(),
                info.formId(),
                info.applicationOpen()
            );
        }
    }
}
