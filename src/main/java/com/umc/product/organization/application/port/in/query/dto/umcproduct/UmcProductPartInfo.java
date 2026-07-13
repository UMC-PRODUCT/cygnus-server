package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import com.umc.product.organization.domain.UmcProductPart;

public record UmcProductPartInfo(
    Long partId,
    Long chapterId,
    UmcProductChapterInfo chapter,
    String code,
    String name,
    String description,
    int sortOrder,
    boolean active
) {
    public static UmcProductPartInfo from(UmcProductPart part) {
        return from(part, UmcProductChapterInfo.from(part.getChapter()));
    }

    public static UmcProductPartInfo from(UmcProductPart part, UmcProductChapterInfo chapter) {
        return new UmcProductPartInfo(
            part.getId(),
            part.getChapter().getId(),
            chapter,
            part.getCode(),
            part.getName(),
            part.getDescription(),
            part.getSortOrder(),
            part.isActive()
        );
    }
}
