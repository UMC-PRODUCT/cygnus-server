package com.umc.product.organization.application.port.in.query.dto.school;

import java.time.Instant;

public record SchoolChapterInfo(
    Long chapterId,
    String chapterName,
    String schoolName,
    String shortName,
    Long schoolId,
    String remark,
    String logoImageId, // 아직 파일 URL이 아닌 ID
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
