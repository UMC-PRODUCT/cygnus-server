package com.umc.product.organization.application.port.in.query.dto.school;

import java.time.Instant;
import java.util.List;

import com.umc.product.organization.domain.enums.SchoolLinkType;


public record SchoolDetailInfo(
    Long chapterId,
    String chapterName,
    String schoolName,
    String shortName,
    Long schoolId,
    String remark,
    String logoImageUrl,
    List<SchoolLinkItem> links,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public record SchoolLinkItem(
        String title,
        SchoolLinkType type,
        String url
    ) {
    }

}
