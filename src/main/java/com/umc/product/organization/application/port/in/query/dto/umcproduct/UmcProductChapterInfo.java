package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import com.umc.product.organization.domain.UmcProductChapter;

public record UmcProductChapterInfo(
    Long chapterId,
    String code,
    String name,
    String description,
    int sortOrder,
    boolean active
) {
    public static UmcProductChapterInfo from(UmcProductChapter chapter) {
        return new UmcProductChapterInfo(
            chapter.getId(),
            chapter.getCode(),
            chapter.getName(),
            chapter.getDescription(),
            chapter.getSortOrder(),
            chapter.isActive()
        );
    }
}
