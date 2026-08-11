package com.umc.product.organization.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

/**
 * 스키마 {@code GisuChapter} 타입 응답. Node가 아니며 chapterId는 Chapter 전역 ID로 인코딩한다.
 * gisuId/rawChapterId는 배치 로딩용 raw ID로 스키마에 노출하지 않는다.
 */
public record GisuChapterGraphQlResponse(
    Long gisuId,
    Long rawChapterId,
    String chapterName
) {

    public static GisuChapterGraphQlResponse from(Long gisuId, ChapterInfo info) {
        return new GisuChapterGraphQlResponse(gisuId, info.id(), info.name());
    }

    public String chapterId() {
        return GlobalId.encode(GlobalIdTypes.CHAPTER, rawChapterId);
    }
}
