package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundAuthorInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기수와 현재 학교 소속을 반영한 모집 시즌 목록 응답")
public record RecruitingSeasonSummaryResponse(
    @Schema(description = "모집 시즌 ID", example = "10") Long seasonId,
    @Schema(description = "기수 ID", example = "15") Long gisuId,
    @Schema(description = "현재 지부 ID", example = "2") Long chapterId,
    @Schema(description = "현재 지부명", example = "서울 지부") String chapterName,
    @Schema(description = "학교 ID", example = "3") Long schoolId,
    @Schema(description = "학교명", example = "한국대학교") String schoolName,
    @Schema(description = "시즌 운영진 공유 메모") String memo,
    @Schema(description = "시즌에 속한 모집 차수")
    List<RoundResponse> rounds
) {

    public static RecruitingSeasonSummaryResponse from(RecruitingSeasonSummaryInfo info) {
        return new RecruitingSeasonSummaryResponse(
            info.seasonId(),
            info.gisuId(),
            info.chapterId(),
            info.chapterName(),
            info.schoolId(),
            info.schoolName(),
            info.memo(),
            info.rounds().stream()
                .map(RoundResponse::from)
                .toList()
        );
    }

    @Schema(description = "목록용 모집 차수 상세")
    public record RoundResponse(
        Long id,
        String title,
        RecruitingRoundType type,
        Integer roundNo,
        RecruitingRoundStatus status,
        List<ChallengerTrack> recruitableTracks,
        boolean secondChoiceEnabled,
        Instant documentStartAt,
        Instant documentEndAt,
        Instant documentResultPublishedAt,
        boolean interviewRequired,
        Instant interviewStartAt,
        Instant interviewEndAt,
        Instant finalResultPublishedAt,
        Long availabilityFormId,
        Long availabilityScheduleQuestionId,
        String announcement,
        String contactText,
        Instant createdAt,
        AuthorResponse author,
        boolean hasApplicants
    ) {

        private static RoundResponse from(RecruitingRoundDetailInfo detail) {
            RecruitingRoundConfigurationInfo info = detail.configuration();
            return new RoundResponse(
                info.id(),
                info.title(),
                info.type(),
                info.roundNo(),
                info.status(),
                info.recruitableTracks(),
                info.secondChoiceEnabled(),
                info.documentStartAt(),
                info.documentEndAt(),
                info.documentResultPublishedAt(),
                info.interviewRequired(),
                info.interviewStartAt(),
                info.interviewEndAt(),
                info.finalResultPublishedAt(),
                info.availabilityFormId(),
                info.availabilityScheduleQuestionId(),
                info.announcement(),
                info.contactText(),
                detail.createdAt(),
                AuthorResponse.from(detail.author()),
                detail.hasApplicants()
            );
        }
    }

    @Schema(description = "모집 차수 작성자")
    public record AuthorResponse(Long memberId, String name, String nickname, String schoolName) {

        private static AuthorResponse from(RecruitingRoundAuthorInfo info) {
            return info == null ? null : new AuthorResponse(
                info.memberId(),
                info.name(),
                info.nickname(),
                info.schoolName()
            );
        }
    }
}
