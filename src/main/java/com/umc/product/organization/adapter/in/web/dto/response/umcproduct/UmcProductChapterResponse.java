package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;

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
