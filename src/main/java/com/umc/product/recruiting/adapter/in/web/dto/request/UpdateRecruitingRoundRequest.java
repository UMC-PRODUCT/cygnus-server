package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모집 차수 설정 변경 요청")
public record UpdateRecruitingRoundRequest(
    @Schema(description = "모집 대상 트랙 목록") @NotEmpty List<ChallengerTrack> recruitableTracks,
    @Schema(description = "2지망 지원 허용 여부", example = "true") boolean secondChoiceEnabled,
    @Schema(description = "서류 접수 시작 시각") @NotNull Instant documentStartAt,
    @Schema(description = "서류 접수 종료 시각") @NotNull Instant documentEndAt,
    @Schema(description = "서류 결과 공개 시각") @NotNull Instant documentResultPublishedAt,
    @Schema(description = "면접 진행 여부", example = "true") boolean interviewRequired,
    @Schema(description = "면접 기간 시작 시각") Instant interviewStartAt,
    @Schema(description = "면접 기간 종료 시각") Instant interviewEndAt,
    @Schema(description = "최종 결과 공개 시각") @NotNull Instant finalResultPublishedAt,
    @Schema(description = "면접 가능 일정 Form ID", example = "100") Long availabilityFormId,
    @Schema(description = "지원자 안내 문구") String announcement,
    @Schema(description = "문의 연락처") String contactText
) {

    public UpdateRecruitingRoundCommand toCommand(Long seasonId, Long roundId) {
        return UpdateRecruitingRoundCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .configuration(RecruitingRoundConfigurationCommand.of(
                recruitableTracks,
                secondChoiceEnabled,
                documentStartAt,
                documentEndAt,
                documentResultPublishedAt,
                interviewRequired,
                interviewStartAt,
                interviewEndAt,
                finalResultPublishedAt,
                availabilityFormId,
                announcement,
                contactText
            ))
            .build();
    }

    @AssertTrue(message = "면접 여부에 맞는 면접 기간과 availability form이 필요합니다.") public boolean isInterviewConfigurationValid() {
        if (interviewRequired) {
            return interviewStartAt != null && interviewEndAt != null;
        }
        return interviewStartAt == null && interviewEndAt == null && availabilityFormId == null;
    }
}
