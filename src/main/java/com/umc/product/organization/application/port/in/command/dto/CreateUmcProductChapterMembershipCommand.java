package com.umc.product.organization.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.organization.domain.enums.UmcProductPosition;

public record CreateUmcProductChapterMembershipCommand(
    Long umcProductMemberId,
    Long requesterMemberId,
    Long chapterId,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static CreateUmcProductChapterMembershipCommand of(
        Long umcProductMemberId,
        Long requesterMemberId,
        Long chapterId,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new CreateUmcProductChapterMembershipCommand(
            umcProductMemberId, requesterMemberId, chapterId, position, responsibilityTitle,
            responsibilityDescription, startDate, endDate
        );
    }
}
