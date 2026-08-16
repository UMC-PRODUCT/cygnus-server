package com.umc.product.organization.application.port.in.query.dto.school;

public record SchoolChapterNameInfo(
    Long chapterId,
    String chapterName,
    String schoolName,
    Long schoolId
) {
}
