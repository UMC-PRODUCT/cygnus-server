package com.umc.product.organization.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.umc.product.organization.application.port.in.command.dto.CreateUmcProductChapterMembershipCommand;
import com.umc.product.organization.domain.enums.UmcProductPosition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUmcProductChapterMembershipRequest(
    @NotNull Long chapterId,
    @NotNull UmcProductPosition position,
    @Size(max = 200) String responsibilityTitle,
    @Size(max = 1000) String responsibilityDescription,
    @NotNull @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-07-13")
    LocalDate startDate,
    @UmcProductDateFormat
    @Schema(type = "string", format = "date", example = "2026-12-31", nullable = true)
    LocalDate endDate
) {
    public CreateUmcProductChapterMembershipCommand toCommand(
        Long umcProductMemberId,
        Long requesterMemberId
    ) {
        return CreateUmcProductChapterMembershipCommand.of(
            umcProductMemberId,
            requesterMemberId,
            chapterId,
            position,
            responsibilityTitle,
            responsibilityDescription,
            startDate,
            endDate
        );
    }
}
