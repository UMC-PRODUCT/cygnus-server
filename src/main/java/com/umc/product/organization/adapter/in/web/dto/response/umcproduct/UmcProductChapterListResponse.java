package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductChapterInfo;

public record UmcProductChapterListResponse(
    List<UmcProductChapterResponse> chapters
) {
    public static UmcProductChapterListResponse from(List<UmcProductChapterInfo> infos) {
        return new UmcProductChapterListResponse(
            infos.stream().map(UmcProductChapterResponse::from).toList()
        );
    }
}
