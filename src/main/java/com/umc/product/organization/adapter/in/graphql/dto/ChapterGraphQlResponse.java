package com.umc.product.organization.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;

/**
 * 스키마 {@code Chapter} 타입 응답. chapterId는 raw ID로 스키마에 노출하지 않는다.
 */
public record ChapterGraphQlResponse(
    Long chapterId,
    String name
) implements RelayNode {

    public static ChapterGraphQlResponse from(ChapterInfo info) {
        return new ChapterGraphQlResponse(info.id(), info.name());
    }

    @Override
    public String id() {
        return GlobalId.encode(GlobalIdTypes.CHAPTER, chapterId);
    }
}
