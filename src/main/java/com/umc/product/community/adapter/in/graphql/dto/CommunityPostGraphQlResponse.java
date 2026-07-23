package com.umc.product.community.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.community.application.port.in.query.dto.PostDetailInfo;
import com.umc.product.community.application.port.in.query.dto.PostInfo;
import com.umc.product.community.domain.enums.Category;

public record CommunityPostGraphQlResponse(
    Long postId,
    String title,
    String content,
    Category category,
    Long authorMemberId,
    Long authorChallengerId,
    Lightning lightning,
    Instant createdAt,
    int commentCount,
    int likeCount,
    Viewer viewer
) {

    public static CommunityPostGraphQlResponse from(PostInfo info) {
        return new CommunityPostGraphQlResponse(
            info.postId(),
            info.title(),
            info.content(),
            info.category(),
            info.authorMemberId(),
            info.authorChallengerId(),
            Lightning.from(info.meetAt(), info.location(), info.maxParticipants(), info.openChatUrl()),
            info.createdAt(),
            info.commentCount(),
            info.likeCount(),
            new Viewer(info.isLiked(), false, 0)
        );
    }

    public static CommunityPostGraphQlResponse from(PostDetailInfo info) {
        return new CommunityPostGraphQlResponse(
            info.postId(),
            info.title(),
            info.content(),
            info.category(),
            info.authorMemberId(),
            info.authorChallengerId(),
            Lightning.from(info.meetAt(), info.location(), info.maxParticipants(), info.openChatUrl()),
            info.createdAt(),
            info.commentCount(),
            info.likeCount(),
            new Viewer(info.isLiked(), info.isScrapped(), info.scrapCount())
        );
    }

    public record Lightning(
        Instant meetAt,
        String location,
        Integer maxParticipants,
        String openChatUrl
    ) {

        private static Lightning from(
            Instant meetAt,
            String location,
            Integer maxParticipants,
            String openChatUrl
        ) {
            return meetAt == null ? null : new Lightning(meetAt, location, maxParticipants, openChatUrl);
        }
    }

    public record Viewer(
        boolean liked,
        boolean scrapped,
        int scrapCount
    ) {
    }
}
