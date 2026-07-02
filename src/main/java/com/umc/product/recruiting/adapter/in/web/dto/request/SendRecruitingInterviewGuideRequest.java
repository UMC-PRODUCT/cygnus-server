package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.command.dto.SendRecruitingInterviewGuideCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "면접 안내 메일 발송 요청")
public record SendRecruitingInterviewGuideRequest(
    @Schema(description = "안내 메일 수신자 이메일", example = "applicant@example.com")
    @NotBlank String recipientEmail,
    @Schema(description = "면접 시작 일시", example = "2026-07-10T10:00:00Z")
    @NotNull Instant startsAt,
    @Schema(description = "면접 장소 또는 접속 링크", example = "Zoom Room A")
    String location
) {

    public SendRecruitingInterviewGuideCommand toCommand(Long applicationId) {
        return SendRecruitingInterviewGuideCommand.builder()
            .applicationId(applicationId)
            .recipientEmail(recipientEmail)
            .startsAt(startsAt)
            .location(location)
            .build();
    }
}
