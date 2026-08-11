package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.time.LocalDate;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterMembershipInfo;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;

import io.swagger.v3.oas.annotations.media.Schema;

public record UmcProductChapterMembershipResponse(
    Long chapterMembershipId,
    Long activityPeriodId,
    Long chapterId,
    UmcProductChapterResponse chapter,
    UmcProductPosition position,
    String positionName,
    String responsibilityTitle,
    String responsibilityDescription,
    @Schema(type = "string", format = "date") LocalDate startDate,
    @Schema(type = "string", format = "date", nullable = true) LocalDate endDate
) {
    public static UmcProductChapterMembershipResponse from(UmcProductChapterMembershipInfo info) {
        return new UmcProductChapterMembershipResponse(
            info.chapterMembershipId(),
            info.activityPeriodId(),
            info.chapterId(),
            info.chapter() == null ? null : UmcProductChapterResponse.from(info.chapter()),
            info.position(),
            info.positionName(),
            info.responsibilityTitle(),
            info.responsibilityDescription(),
            info.startDate(),
            info.endDate()
        );
    }
}
