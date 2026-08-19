package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationCommand;

import jakarta.validation.constraints.NotBlank;

public record GuestParticipationRequest(
    @NotBlank String admissionCode
) {
    public StartDemodayGuestParticipationCommand toCommand(Long pollId, Long existingEntryCodeId) {
        return new StartDemodayGuestParticipationCommand(pollId, admissionCode, existingEntryCodeId);
    }
}
