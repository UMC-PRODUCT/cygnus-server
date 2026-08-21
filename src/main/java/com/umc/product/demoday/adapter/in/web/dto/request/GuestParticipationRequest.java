package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuestParticipationRequest(
    @NotBlank String admissionCode,
    @Schema(
        description = "동일한 입장 시도의 안전한 재시도를 위한 UUID v4. 기존 클라이언트는 생략할 수 있습니다.",
        example = "0f43f02a-6ecf-4bb3-82ce-625029bd3e09",
        nullable = true
    )
    @Pattern(
        regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
        message = "requestId는 소문자 UUID v4 형식이어야 합니다."
    )
    String requestId
) {
    public StartDemodayGuestParticipationCommand toCommand(Long pollId, Long existingEntryCodeId) {
        return new StartDemodayGuestParticipationCommand(pollId, admissionCode, requestId, existingEntryCodeId);
    }
}
