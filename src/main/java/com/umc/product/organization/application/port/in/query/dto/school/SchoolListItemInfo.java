package com.umc.product.organization.application.port.in.query.dto.school;

import java.time.Instant;

public record SchoolListItemInfo(
    Long schoolId,
    String schoolName,
    String shortName,
    Long chapterId,        // 활성 기수에 속하지 않으면 null
    String chapterName,    // 활성 기수에 속하지 않으면 null
    Instant createdAt,
    boolean isActive,      // 활성 기수의 ChapterSchool 존재 여부
    String remark,
    String logoImageUrl
) {

    public SchoolListItemInfo withLogoImageUrl(String logoImageUrl) {
        return new SchoolListItemInfo(
            schoolId, schoolName, shortName, chapterId, chapterName,
            createdAt, isActive, remark, logoImageUrl
        );
    }
}
