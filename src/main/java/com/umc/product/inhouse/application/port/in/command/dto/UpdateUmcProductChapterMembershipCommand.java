package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record UpdateUmcProductChapterMembershipCommand(
    Long umcProductMemberId,
    Long chapterMembershipId,
    Long requesterMemberId,
    Long chapterId,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UpdateUmcProductChapterMembershipCommand of(
        Long umcProductMemberId,
        Long chapterMembershipId,
        Long requesterMemberId,
        Long chapterId,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return new UpdateUmcProductChapterMembershipCommand(
            umcProductMemberId, chapterMembershipId, requesterMemberId, chapterId, position,
            responsibilityTitle, responsibilityDescription, startDate, endDate
        );
    }
}
