package com.umc.product.organization.adapter.in.graphql.dto;

import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

public record ChapterGraphQlResponse(
    Long id,
    String name
) {

    public static ChapterGraphQlResponse from(ChapterInfo info) {
        return new ChapterGraphQlResponse(info.id(), info.name());
    }
}
