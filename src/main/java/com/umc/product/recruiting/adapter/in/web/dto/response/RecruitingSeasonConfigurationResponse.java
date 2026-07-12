package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonTrackQuotaInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모집 시즌의 쿼터와 차수 설정 응답")
public record RecruitingSeasonConfigurationResponse(
    @Schema(description = "모집 시즌 ID", example = "10") Long id,
    @Schema(description = "기수 ID", example = "15") Long gisuId,
    @Schema(description = "학교 ID", example = "3") Long schoolId,
    @Schema(description = "시즌 상태", example = "OPEN") RecruitingSeasonStatus status,
    @Schema(description = "트랙별 목표 인원") List<QuotaResponse> quotas,
    @Schema(description = "모집 차수 설정") List<RoundResponse> rounds
) {

    public static RecruitingSeasonConfigurationResponse from(RecruitingSeasonConfigurationInfo info) {
        return new RecruitingSeasonConfigurationResponse(
            info.id(),
            info.gisuId(),
            info.schoolId(),
            info.status(),
            info.quotas().stream().map(QuotaResponse::from).toList(),
            info.rounds().stream().map(RoundResponse::from).toList()
        );
    }

    @Schema(description = "트랙별 모집 목표 인원")
    public record QuotaResponse(
        @Schema(description = "모집 트랙", example = "PLAN") ChallengerTrack track,
        @Schema(description = "목표 인원", example = "5") Integer targetCount
    ) {

        private static QuotaResponse from(RecruitingSeasonTrackQuotaInfo info) {
            return new QuotaResponse(info.track(), info.targetCount());
        }
    }

    @Schema(description = "모집 차수 설정")
    public record RoundResponse(
        @Schema(description = "모집 차수 ID", example = "20") Long id,
        @Schema(description = "모집 차수 유형", example = "REGULAR") RecruitingRoundType type,
        @Schema(description = "추가모집 차수 번호", example = "1") Integer roundNo,
        @Schema(description = "모집 차수 상태", example = "OPEN") RecruitingRoundStatus status,
        @Schema(description = "모집 대상 트랙 목록") List<ChallengerTrack> recruitableTracks,
        @Schema(description = "2지망 지원 허용 여부", example = "true") boolean secondChoiceEnabled,
        @Schema(description = "서류 접수 시작 시각") Instant documentStartAt,
        @Schema(description = "서류 접수 종료 시각") Instant documentEndAt,
        @Schema(description = "서류 결과 공개 시각") Instant documentResultPublishedAt,
        @Schema(description = "면접 진행 여부", example = "true") boolean interviewRequired,
        @Schema(description = "면접 기간 시작 시각") Instant interviewStartAt,
        @Schema(description = "면접 기간 종료 시각") Instant interviewEndAt,
        @Schema(description = "최종 결과 공개 시각") Instant finalResultPublishedAt,
        @Schema(description = "면접 가능 일정 Survey 폼 ID", example = "100") Long availabilityFormId,
        @Schema(description = "지원자 안내 문구") String announcement,
        @Schema(description = "문의 연락처") String contactText
    ) {

        private static RoundResponse from(RecruitingRoundConfigurationInfo info) {
            return new RoundResponse(
                info.id(),
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
                info.announcement(),
                info.contactText()
            );
        }
    }
}
