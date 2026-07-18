package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기수와 현재 학교 소속을 반영한 모집 시즌 목록 응답")
public record RecruitingSeasonSummaryResponse(
    @Schema(description = "모집 시즌 ID", example = "10") Long seasonId,
    @Schema(description = "기수 ID", example = "15") Long gisuId,
    @Schema(description = "현재 지부 ID", example = "2") Long chapterId,
    @Schema(description = "현재 지부명", example = "서울 지부") String chapterName,
    @Schema(description = "학교 ID", example = "3") Long schoolId,
    @Schema(description = "학교명", example = "한국대학교") String schoolName,
    @Schema(description = "시즌 상태", example = "ACTIVE") RecruitingSeasonStatus status,
    @Schema(description = "시즌에 속한 모집 차수")
    List<RecruitingSeasonConfigurationResponse.RoundResponse> rounds
) {

    public static RecruitingSeasonSummaryResponse from(RecruitingSeasonSummaryInfo info) {
        return new RecruitingSeasonSummaryResponse(
            info.seasonId(),
            info.gisuId(),
            info.chapterId(),
            info.chapterName(),
            info.schoolId(),
            info.schoolName(),
            info.status(),
            info.rounds().stream()
                .map(RecruitingSeasonConfigurationResponse.RoundResponse::from)
                .toList()
        );
    }
}
