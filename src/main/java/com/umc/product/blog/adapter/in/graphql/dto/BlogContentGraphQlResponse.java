package com.umc.product.blog.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.blog.application.port.in.query.dto.BlogContentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogContentSummaryInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogHashtagInfo;
import com.umc.product.blog.domain.BlogContentStatus;
import com.umc.product.blog.domain.BlogContentType;

public record BlogContentGraphQlResponse(
    Long id,
    BlogContentType type,
    String slug,
    String title,
    String summary,
    String thumbnailUrl,
    String content,
    BlogContentStatus status,
    Long authorMemberId,
    Instant publishedAt,
    Instant updatedAt,
    String canonicalPath,
    String seoTitle,
    String seoDescription,
    String ogImageUrl,
    List<BlogSeriesGraphQlResponse> series,
    List<BlogHashtagInfo> hashtags,
    boolean canEdit,
    boolean canDelete
) {

    public static BlogContentGraphQlResponse from(BlogContentInfo info) {
        return new BlogContentGraphQlResponse(
            info.id(),
            info.type(),
            info.slug(),
            info.title(),
            info.summary(),
            info.thumbnailUrl(),
            info.content(),
            info.status(),
            info.author().id(),
            info.publishedAt(),
            info.updatedAt(),
            info.canonicalPath(),
            info.seoTitle(),
            info.seoDescription(),
            info.ogImageUrl(),
            info.series().stream().map(BlogSeriesGraphQlResponse::from).toList(),
            info.hashtags(),
            info.canEdit(),
            info.canDelete()
        );
    }

    public static BlogContentGraphQlResponse from(BlogContentSummaryInfo info) {
        return new BlogContentGraphQlResponse(
            info.id(),
            info.type(),
            info.slug(),
            info.title(),
            info.summary(),
            info.thumbnailUrl(),
            null,
            info.status(),
            info.author().id(),
            info.publishedAt(),
            info.updatedAt(),
            info.canonicalPath(),
            info.seoTitle(),
            info.seoDescription(),
            info.ogImageUrl(),
            info.series().stream().map(BlogSeriesGraphQlResponse::from).toList(),
            info.hashtags(),
            info.canEdit(),
            info.canDelete()
        );
    }
}
