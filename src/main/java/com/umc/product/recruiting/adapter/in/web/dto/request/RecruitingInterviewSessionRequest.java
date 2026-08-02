package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "면접 세션 생성 또는 수정 요청")
public record RecruitingInterviewSessionRequest(
    @Schema(description = "세션 이름", example = "1차 온라인 면접") @NotBlank @Size(max = 100) String name,
    @Schema(description = "세션 시작 시각") @NotNull Instant startsAt,
    @Schema(description = "세션 종료 시각") @NotNull Instant endsAt,
    @Schema(description = "면접 방식", example = "ONLINE") @NotNull RecruitingInterviewMode mode,
    @Schema(description = "면접 장소 또는 접속 링크", example = "https://meet.example.com/umc")
    @NotBlank @Size(max = 255) String location
) {

    @AssertTrue(message = "면접 종료 시각은 시작 시각보다 늦어야 합니다.") public boolean isPeriodValid() {
        return startsAt == null || endsAt == null || startsAt.isBefore(endsAt);
    }

    public CreateRecruitingInterviewSessionCommand toCreateCommand(Long roundId, Long requesterMemberId) {
        return CreateRecruitingInterviewSessionCommand.of(
            roundId, requesterMemberId, name, startsAt, endsAt, mode, location
        );
    }

    public UpdateRecruitingInterviewSessionCommand toUpdateCommand(
        Long sessionId,
        Long roundId,
        Long requesterMemberId
    ) {
        return UpdateRecruitingInterviewSessionCommand.of(
            sessionId, roundId, requesterMemberId, name, startsAt, endsAt, mode, location
        );
    }
}
