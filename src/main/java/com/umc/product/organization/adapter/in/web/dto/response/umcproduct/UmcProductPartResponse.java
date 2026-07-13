package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartInfo;

public record UmcProductPartResponse(
    Long partId,
    Long chapterId,
    UmcProductChapterResponse chapter,
    String code,
    String name,
    String description,
    int sortOrder,
    boolean active
) {
    public static UmcProductPartResponse from(UmcProductPartInfo info) {
        return new UmcProductPartResponse(
            info.partId(),
            info.chapterId(),
            info.chapter() == null ? null : UmcProductChapterResponse.from(info.chapter()),
            info.code(),
            info.name(),
            info.description(),
            info.sortOrder(),
            info.active()
        );
    }
}
