package com.umc.product.blog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService;
import com.umc.product.blog.application.port.in.query.GetBlogSeriesUseCase;
import com.umc.product.blog.application.port.in.query.dto.BlogContentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesListQuery;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogSeriesPort;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.blog.domain.BlogDomainException;
import com.umc.product.blog.domain.BlogErrorCode;
import com.umc.product.blog.domain.BlogSeries;
import com.umc.product.blog.domain.BlogSeriesSort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogSeriesQueryService implements GetBlogSeriesUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final LoadBlogSeriesPort loadBlogSeriesPort;
    private final LoadBlogContentPort loadBlogContentPort;
    private final BlogSeriesInfoAssembler seriesInfoAssembler;
    private final BlogContentInfoAssembler contentInfoAssembler;
    private final BlogPolicyAuthorizationService policyAuthorizationService;

    @Override
    public BlogSeriesCursorInfo getPublicSeries(BlogSeriesListQuery query) {
        BlogContentType type = parseOptionalType(query.type());
        BlogSeriesSort sort = BlogSeriesSort.fromSeriesList(query.sort());
        int size = normalizeSize(query.size());
        List<BlogSeries> rows = loadBlogSeriesPort.listPublicSeries(type, sort, query.cursor(), size + 1);
        boolean hasNext = rows.size() > size;
        List<BlogSeries> page = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new BlogSeriesCursorInfo(
            seriesInfoAssembler.assembleSummaries(page, query.viewerMemberId(), isSuperAdmin(query.viewerMemberId())),
            nextCursor,
            hasNext
        );
    }

    @Override
    public BlogSeriesInfo getPublicSeries(String typeValue, String slug, Long viewerMemberId) {
        BlogContentType type = BlogContentType.fromPath(typeValue);
        BlogSeries series = loadBlogSeriesPort.findSeriesByTypeAndSlug(type, slug)
            .filter(candidate -> !candidate.isDeleted())
            .filter(candidate -> loadBlogSeriesPort.hasPublishedContent(candidate.getId()))
            .orElseThrow(() -> new BlogDomainException(BlogErrorCode.SERIES_NOT_FOUND));
        return seriesInfoAssembler.assemble(series, viewerMemberId, isSuperAdmin(viewerMemberId));
    }

    @Override
    public BlogSeriesInfo getPreview(Long seriesId, Long viewerMemberId) {
        BlogSeries series = loadBlogSeriesPort.findSeriesById(seriesId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new BlogDomainException(BlogErrorCode.SERIES_NOT_FOUND));
        return seriesInfoAssembler.assemble(series, viewerMemberId, isSuperAdmin(viewerMemberId));
    }

    @Override
    public BlogContentCursorInfo getPublicSeriesContents(
        String typeValue,
        String slug,
        Long cursor,
        int requestedSize,
        String sortValue,
        Long viewerMemberId
    ) {
        BlogSeriesSort.fromSeriesContents(sortValue);
        BlogContentType type = BlogContentType.fromPath(typeValue);
        BlogSeries series = loadBlogSeriesPort.findSeriesByTypeAndSlug(type, slug)
            .filter(candidate -> !candidate.isDeleted())
            .filter(candidate -> loadBlogSeriesPort.hasPublishedContent(candidate.getId()))
            .orElseThrow(() -> new BlogDomainException(BlogErrorCode.SERIES_NOT_FOUND));

        int size = normalizeSize(requestedSize);
        List<BlogContent> rows = loadBlogContentPort.listPublicSeriesContents(series.getId(), cursor, size + 1);
        boolean hasNext = rows.size() > size;
        List<BlogContent> page = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new BlogContentCursorInfo(
            contentInfoAssembler.assembleSummaries(page, viewerMemberId, isSuperAdmin(viewerMemberId)),
            nextCursor,
            hasNext
        );
    }

    private BlogContentType parseOptionalType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return BlogContentType.fromPath(value);
    }

    private int normalizeSize(int requestedSize) {
        if (requestedSize <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(requestedSize, MAX_SIZE);
    }

    private boolean isSuperAdmin(Long memberId) {
        return policyAuthorizationService.isSuperAdminViewer(memberId);
    }
}
