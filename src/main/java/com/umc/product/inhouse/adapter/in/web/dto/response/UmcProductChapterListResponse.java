package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductChapterInfo;

public record UmcProductChapterListResponse(
    List<UmcProductChapterResponse> chapters
) {
    public static UmcProductChapterListResponse from(List<UmcProductChapterInfo> infos) {
        return new UmcProductChapterListResponse(
            infos.stream().map(UmcProductChapterResponse::from).toList()
        );
    }
}
