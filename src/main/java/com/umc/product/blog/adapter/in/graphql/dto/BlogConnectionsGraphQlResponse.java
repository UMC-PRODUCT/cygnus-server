package com.umc.product.blog.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.blog.application.port.in.query.dto.BlogCommentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogContentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogHashtagCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogHashtagInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesCursorInfo;

public final class BlogConnectionsGraphQlResponse {

    private BlogConnectionsGraphQlResponse() {
    }

    public record Contents(
        List<BlogContentGraphQlResponse> content,
        Long nextCursor,
        boolean hasNext
    ) {

        public static Contents from(BlogContentCursorInfo info) {
            return new Contents(
                info.content().stream().map(BlogContentGraphQlResponse::from).toList(),
                info.nextCursor(),
                info.hasNext()
            );
        }
    }

    public record Series(
        List<BlogSeriesGraphQlResponse> content,
        Long nextCursor,
        boolean hasNext
    ) {

        public static Series from(BlogSeriesCursorInfo info) {
            return new Series(
                info.content().stream().map(BlogSeriesGraphQlResponse::from).toList(),
                info.nextCursor(),
                info.hasNext()
            );
        }
    }

    public record Hashtags(
        List<BlogHashtagInfo> content,
        Long nextCursor,
        boolean hasNext
    ) {

        public static Hashtags from(BlogHashtagCursorInfo info) {
            return new Hashtags(info.content(), info.nextCursor(), info.hasNext());
        }
    }

    public record Comments(
        List<BlogCommentInfo> content,
        Long nextCursor,
        boolean hasNext
    ) {

        public static Comments from(BlogCommentCursorInfo info) {
            return new Comments(info.content(), info.nextCursor(), info.hasNext());
        }
    }
}
