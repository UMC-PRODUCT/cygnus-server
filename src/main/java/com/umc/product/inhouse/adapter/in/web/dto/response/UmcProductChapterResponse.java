package com.umc.product.inhouse.adapter.in.web.dto.response;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterInfo;

public record UmcProductChapterResponse(
    Long chapterId,
    String code,
    String name,
    String description,
    int sortOrder,
    boolean active
) {
    public static UmcProductChapterResponse from(UmcProductChapterInfo info) {
        return new UmcProductChapterResponse(
            info.chapterId(),
            info.code(),
            info.name(),
            info.description(),
            info.sortOrder(),
            info.active()
        );
    }
}
