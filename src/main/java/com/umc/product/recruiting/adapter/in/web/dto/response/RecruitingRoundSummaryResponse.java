package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기수, 학교와 현재 지부 소속을 포함한 모집 차수 목록 응답")
public record RecruitingRoundSummaryResponse(
    @Schema(description = "모집 시즌 ID", example = "10") Long seasonId,
    @Schema(description = "기수 ID", example = "15") Long gisuId,
    @Schema(description = "현재 지부 ID", example = "2") Long chapterId,
    @Schema(description = "현재 지부명", example = "서울 지부") String chapterName,
    @Schema(description = "학교 ID", example = "3") Long schoolId,
    @Schema(description = "학교명", example = "한국대학교") String schoolName,
    @Schema(description = "모집 차수 ID", example = "20") Long id,
    @Schema(description = "모집 차수 유형", example = "REGULAR") RecruitingRoundType type,
    @Schema(description = "정규/추가모집 차수 번호", example = "1") Integer roundNo,
    @Schema(description = "모집 차수 상태", example = "OPEN") RecruitingRoundStatus status,
    @Schema(description = "모집 대상 트랙") List<ChallengerTrack> recruitableTracks,
    @Schema(description = "2지망 지원 허용 여부", example = "true") boolean secondChoiceEnabled,
    @Schema(description = "서류 접수 시작 시각") Instant documentStartAt,
    @Schema(description = "서류 접수 종료 시각") Instant documentEndAt,
    @Schema(description = "서류 결과 공개 시각") Instant documentResultPublishedAt,
    @Schema(description = "면접 진행 여부", example = "true") boolean interviewRequired,
    @Schema(description = "면접 기간 시작 시각") Instant interviewStartAt,
    @Schema(description = "면접 기간 종료 시각") Instant interviewEndAt,
    @Schema(description = "최종 결과 공개 시각") Instant finalResultPublishedAt,
    @Schema(description = "면접 가능 일정 Form ID", example = "100") Long availabilityFormId,
    @Schema(description = "지원자 안내 문구") String announcement,
    @Schema(description = "문의 연락처") String contactText
) {

    public static RecruitingRoundSummaryResponse from(RecruitingRoundSummaryInfo info) {
        RecruitingRoundConfigurationInfo round = info.round();
        return new RecruitingRoundSummaryResponse(
            info.seasonId(),
            info.gisuId(),
            info.chapterId(),
            info.chapterName(),
            info.schoolId(),
            info.schoolName(),
            round.id(),
            round.type(),
            round.roundNo(),
            round.status(),
            round.recruitableTracks(),
            round.secondChoiceEnabled(),
            round.documentStartAt(),
            round.documentEndAt(),
            round.documentResultPublishedAt(),
            round.interviewRequired(),
            round.interviewStartAt(),
            round.interviewEndAt(),
            round.finalResultPublishedAt(),
            round.availabilityFormId(),
            round.announcement(),
            round.contactText()
        );
    }
}
