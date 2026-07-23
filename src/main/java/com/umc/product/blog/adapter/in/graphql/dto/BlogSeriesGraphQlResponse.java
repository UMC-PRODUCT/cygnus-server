package com.umc.product.blog.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.blog.application.port.in.query.dto.BlogSeriesInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesSummaryInfo;
import com.umc.product.blog.domain.BlogContentType;

public record BlogSeriesGraphQlResponse(
    Long id,
    BlogContentType type,
    String slug,
    String title,
    String description,
    String thumbnailUrl,
    Long authorMemberId,
    int contentCount,
    Instant updatedAt,
    String canonicalPath,
    String seoTitle,
    String seoDescription,
    String ogImageUrl,
    boolean canEdit,
    boolean canDelete
) {

    public static BlogSeriesGraphQlResponse from(BlogSeriesInfo info) {
        return new BlogSeriesGraphQlResponse(
            info.id(),
            info.type(),
            info.slug(),
            info.title(),
            info.description(),
            info.thumbnailUrl(),
            info.author().id(),
            info.contentCount(),
            info.updatedAt(),
            info.canonicalPath(),
            info.seoTitle(),
            info.seoDescription(),
            info.ogImageUrl(),
            info.canEdit(),
            info.canDelete()
        );
    }

    public static BlogSeriesGraphQlResponse from(BlogSeriesSummaryInfo info) {
        return new BlogSeriesGraphQlResponse(
            info.id(),
            info.type(),
            info.slug(),
            info.title(),
            info.description(),
            info.thumbnailUrl(),
            info.author().id(),
            info.contentCount(),
            info.updatedAt(),
            info.canonicalPath(),
            info.seoTitle(),
            info.seoDescription(),
            info.ogImageUrl(),
            info.canEdit(),
            info.canDelete()
        );
    }
}
