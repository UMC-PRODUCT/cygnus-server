package com.umc.product.community.application.port.out.dto;

import java.time.Instant;

import com.umc.product.community.application.port.in.query.dto.PostSearchResult;
import com.umc.product.community.application.port.in.query.dto.PostSearchResult.MatchType;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;

import lombok.Builder;

@Builder
public record PostSearchData(
    Long postId,
    String title,
    String content,
    Category category,
    int likeCount,
    Instant createdAt,
    MatchType matchType,
    int relevanceScore
) {

    public static PostSearchData from(Post post, MatchType matchType, int relevanceScore) {
        return PostSearchData.builder()
            .postId(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .category(post.getCategory())
            .likeCount(post.getLikeCount())
            .createdAt(post.getCreatedAt())

            // TODO: 이거 두 개 필요한게 맞나? 왜 구현되었는지 .. - 경운 to 예은
            .matchType(matchType)
            .relevanceScore(relevanceScore)
            .build();
    }

    // TODO: 하위 객체에서 상위 객체를 생성하고 있습니다. 아키텍쳐 위반입니다 - 경운 to 예은
    public PostSearchResult toResult() {
        return PostSearchResult.of(
            postId,
            title,
            content,
            category,
            likeCount,
            createdAt,
            matchType
        );
    }
}
