package com.umc.product.organization.adapter.in.graphql.dto;

import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

public record GisuChapterGraphQlResponse(
    Long gisuId,
    Long chapterId,
    String chapterName
) {

    public static GisuChapterGraphQlResponse from(Long gisuId, ChapterInfo info) {
        return new GisuChapterGraphQlResponse(gisuId, info.id(), info.name());
    }
}
