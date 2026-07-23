package com.umc.product.blog.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.blog.adapter.in.graphql.dto.BlogConnectionsGraphQlResponse;
import com.umc.product.blog.adapter.in.graphql.dto.BlogContentGraphQlResponse;
import com.umc.product.blog.adapter.in.graphql.dto.BlogGraphQlSort;
import com.umc.product.blog.adapter.in.graphql.dto.BlogSeriesGraphQlResponse;
import com.umc.product.blog.application.port.in.query.GetBlogCommentListUseCase;
import com.umc.product.blog.application.port.in.query.GetBlogContentLikeUseCase;
import com.umc.product.blog.application.port.in.query.GetBlogContentUseCase;
import com.umc.product.blog.application.port.in.query.GetBlogHashtagUseCase;
import com.umc.product.blog.application.port.in.query.GetBlogSeriesUseCase;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentListQuery;
import com.umc.product.blog.application.port.in.query.dto.BlogContentListQuery;
import com.umc.product.blog.application.port.in.query.dto.BlogHashtagInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogLikeInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesListQuery;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BlogGraphQlController {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final GetBlogContentUseCase getBlogContentUseCase;
    private final GetBlogSeriesUseCase getBlogSeriesUseCase;
    private final GetBlogHashtagUseCase getBlogHashtagUseCase;
    private final GetBlogCommentListUseCase getBlogCommentListUseCase;
    private final GetBlogContentLikeUseCase getBlogContentLikeUseCase;
    private final CurrentMemberProvider currentMemberProvider;
    private final GetMemberUseCase getMemberUseCase;

    @QueryMapping
    public BlogConnectionsGraphQlResponse.Contents blogContents(
        @Argument BlogContentType type,
        @Argument String seriesSlug,
        @Argument String hashtagSlug,
        @Argument Long after,
        @Argument Integer first,
        @Argument BlogGraphQlSort.Content sort
    ) {
        return BlogConnectionsGraphQlResponse.Contents.from(
            getBlogContentUseCase.getPublicContents(BlogContentListQuery.of(
                path(type),
                seriesSlug,
                hashtagSlug,
                after,
                size(first),
                sortValue(sort),
                currentMemberProvider.getNullableCurrentMemberId()
            ))
        );
    }

    @QueryMapping
    public BlogContentGraphQlResponse blogContent(
        @Argument BlogContentType type,
        @Argument String slug
    ) {
        return BlogContentGraphQlResponse.from(
            getBlogContentUseCase.getPublicContent(
                type.pathValue(),
                slug,
                currentMemberProvider.getNullableCurrentMemberId()
            )
        );
    }

    @QueryMapping
    public BlogConnectionsGraphQlResponse.Series blogSeriesList(
        @Argument BlogContentType type,
        @Argument Long after,
        @Argument Integer first,
        @Argument BlogGraphQlSort.Series sort
    ) {
        return BlogConnectionsGraphQlResponse.Series.from(
            getBlogSeriesUseCase.getPublicSeries(BlogSeriesListQuery.of(
                path(type),
                after,
                size(first),
                seriesSortValue(sort),
                currentMemberProvider.getNullableCurrentMemberId()
            ))
        );
    }

    @QueryMapping
    public BlogSeriesGraphQlResponse blogSeries(
        @Argument BlogContentType type,
        @Argument String slug
    ) {
        return BlogSeriesGraphQlResponse.from(
            getBlogSeriesUseCase.getPublicSeries(
                type.pathValue(),
                slug,
                currentMemberProvider.getNullableCurrentMemberId()
            )
        );
    }

    @QueryMapping
    public BlogConnectionsGraphQlResponse.Hashtags blogHashtags(
        @Argument BlogContentType type,
        @Argument String query,
        @Argument Long after,
        @Argument Integer first
    ) {
        return BlogConnectionsGraphQlResponse.Hashtags.from(
            getBlogHashtagUseCase.getPublicHashtags(
                path(type),
                query,
                after,
                size(first),
                "contentCount,desc"
            )
        );
    }

    @SchemaMapping(typeName = "BlogContent", field = "viewer")
    public Viewer viewer(BlogContentGraphQlResponse content) {
        BlogLikeInfo like = getBlogContentLikeUseCase.getLikeState(
            content.type().pathValue(),
            content.slug(),
            currentMemberProvider.getNullableCurrentMemberId()
        );
        return new Viewer(like.likedByMe(), like.likeCount(), content.canEdit(), content.canDelete());
    }

    @SchemaMapping(typeName = "BlogContent", field = "comments")
    public BlogConnectionsGraphQlResponse.Comments comments(
        BlogContentGraphQlResponse content,
        @Argument Long after,
        @Argument Integer first,
        @Argument BlogGraphQlSort.Comment sort
    ) {
        return BlogConnectionsGraphQlResponse.Comments.from(
            getBlogCommentListUseCase.getComments(BlogCommentListQuery.of(
                content.type().pathValue(),
                content.slug(),
                after,
                size(first),
                commentSortValue(sort),
                currentMemberProvider.getNullableCurrentMemberId()
            ))
        );
    }

    @SchemaMapping(typeName = "BlogSeries", field = "contents")
    public BlogConnectionsGraphQlResponse.Contents seriesContents(
        BlogSeriesGraphQlResponse series,
        @Argument Long after,
        @Argument Integer first
    ) {
        return BlogConnectionsGraphQlResponse.Contents.from(
            getBlogSeriesUseCase.getPublicSeriesContents(
                series.type().pathValue(),
                series.slug(),
                after,
                size(first),
                "displayOrder,asc",
                currentMemberProvider.getNullableCurrentMemberId()
            )
        );
    }

    @SchemaMapping(typeName = "BlogHashtag", field = "contents")
    public BlogConnectionsGraphQlResponse.Contents hashtagContents(
        BlogHashtagInfo hashtag,
        @Argument BlogContentType type,
        @Argument Long after,
        @Argument Integer first
    ) {
        return BlogConnectionsGraphQlResponse.Contents.from(
            getBlogHashtagUseCase.getPublicHashtagContents(
                hashtag.slug(),
                path(type),
                after,
                size(first),
                "publishedAt,desc",
                currentMemberProvider.getNullableCurrentMemberId()
            )
        );
    }

    @BatchMapping(typeName = "BlogContent", field = "author")
    public Map<BlogContentGraphQlResponse, MemberPublicInfo> contentAuthors(
        List<BlogContentGraphQlResponse> contents
    ) {
        return memberMap(contents, BlogContentGraphQlResponse::authorMemberId);
    }

    @BatchMapping(typeName = "BlogSeries", field = "author")
    public Map<BlogSeriesGraphQlResponse, MemberPublicInfo> seriesAuthors(
        List<BlogSeriesGraphQlResponse> series
    ) {
        return memberMap(series, BlogSeriesGraphQlResponse::authorMemberId);
    }

    @BatchMapping(typeName = "BlogComment", field = "author")
    public Map<BlogCommentInfo, MemberPublicInfo> commentAuthors(List<BlogCommentInfo> comments) {
        return memberMap(
            comments,
            comment -> comment.author() == null ? null : comment.author().id()
        );
    }

    private int size(Integer first) {
        int actual = first == null ? DEFAULT_SIZE : first;
        if (actual <= 0 || actual > MAX_SIZE) {
            throw new IllegalArgumentException("first must be between 1 and " + MAX_SIZE);
        }
        return actual;
    }

    private String path(BlogContentType type) {
        return type == null ? null : type.pathValue();
    }

    private String sortValue(BlogGraphQlSort.Content sort) {
        return (sort == null ? BlogGraphQlSort.Content.PUBLISHED_AT_DESC : sort).value();
    }

    private String seriesSortValue(BlogGraphQlSort.Series sort) {
        return (sort == null ? BlogGraphQlSort.Series.CREATED_AT_DESC : sort).value();
    }

    private String commentSortValue(BlogGraphQlSort.Comment sort) {
        return (sort == null ? BlogGraphQlSort.Comment.CREATED_AT_DESC : sort).value();
    }

    private <T> Map<T, MemberPublicInfo> memberMap(List<T> sources, Function<T, Long> memberIdExtractor) {
        Set<Long> memberIds = sources.stream()
            .map(memberIdExtractor)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<T, MemberPublicInfo> result = new LinkedHashMap<>();
        for (T source : sources) {
            result.put(source, membersById.get(memberIdExtractor.apply(source)));
        }
        return result;
    }

    public record Viewer(
        boolean liked,
        int likeCount,
        boolean canEdit,
        boolean canDelete
    ) {
    }
}
