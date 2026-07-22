package com.umc.product.support.fixture;

import com.umc.product.blog.domain.BlogComment;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentStatus;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.blog.domain.BlogSeries;

public final class BlogUnitFixture {

    private BlogUnitFixture() {
    }

    public static BlogContent content() {
        return content("test-content", BlogContentStatus.DRAFT, 1L);
    }

    public static BlogContent content(String slug, BlogContentStatus status, Long authorMemberId) {
        return BlogContent.create(
            BlogContentType.ENGINEERING,
            slug,
            "테스트 콘텐츠",
            "테스트 요약",
            "https://example.com/thumbnail.png",
            "테스트 본문",
            status,
            authorMemberId,
            null,
            null,
            null
        );
    }

    public static BlogSeries series() {
        return series("test-series", 1L);
    }

    public static BlogSeries series(String slug, Long authorMemberId) {
        return BlogSeries.create(
            BlogContentType.ENGINEERING,
            slug,
            "테스트 시리즈",
            "테스트 설명",
            "https://example.com/thumbnail.png",
            authorMemberId,
            null,
            null,
            null
        );
    }

    public static BlogComment comment() {
        return BlogComment.create(1L, null, 1L, false, null, "테스트 댓글");
    }
}
