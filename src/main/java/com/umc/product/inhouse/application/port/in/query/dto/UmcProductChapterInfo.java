package com.umc.product.inhouse.application.port.in.query.dto;

import com.umc.product.inhouse.domain.UmcProductChapter;

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
