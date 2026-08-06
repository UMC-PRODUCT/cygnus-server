package com.umc.product.organization.application.port.in.query.dto.school;

import java.time.Instant;

public record SchoolGisuChapterInfo(
    Long gisuId,
    Long chapterId,
    String chapterName,
    String schoolName,
    String shortName,
    Long schoolId,
    String remark,
    String logoImageId,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
