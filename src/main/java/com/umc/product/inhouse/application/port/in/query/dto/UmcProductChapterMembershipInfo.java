package com.umc.product.inhouse.application.port.in.query.dto;

import java.time.LocalDate;

import com.umc.product.inhouse.domain.UmcProductChapterMembership;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

public record UmcProductChapterMembershipInfo(
    Long chapterMembershipId,
    Long activityPeriodId,
    Long chapterId,
    UmcProductChapterInfo chapter,
    UmcProductPosition position,
    String positionName,
    String responsibilityTitle,
    String responsibilityDescription,
    LocalDate startDate,
    LocalDate endDate
) {
    public static UmcProductChapterMembershipInfo from(
        UmcProductChapterMembership membership,
        UmcProductChapterInfo chapter
    ) {
        return new UmcProductChapterMembershipInfo(
            membership.getId(),
            membership.getMemberActivityPeriod().getId(),
            membership.getChapter().getId(),
            chapter,
            membership.getPosition(),
            membership.getPosition().getDisplayName(),
            membership.getResponsibilityTitle(),
            membership.getResponsibilityDescription(),
            membership.getStartDate(),
            membership.getEndDate()
        );
    }
}
