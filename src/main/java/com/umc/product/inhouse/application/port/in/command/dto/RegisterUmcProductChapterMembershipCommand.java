package com.umc.product.inhouse.application.port.in.command.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record RegisterUmcProductChapterMembershipCommand(
    Long chapterId,
    UmcProductPosition position,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
}
