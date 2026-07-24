package com.umc.product.blog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService;
import com.umc.product.blog.application.port.in.query.GetBlogContentUseCase;
import com.umc.product.blog.application.port.in.query.dto.BlogContentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogContentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogContentListQuery;
import com.umc.product.blog.application.port.in.query.dto.BlogSeoPathInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeoPathsInfo;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogSeoPort;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentSort;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.blog.domain.BlogDomainException;
import com.umc.product.blog.domain.BlogErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogContentQueryService implements GetBlogContentUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final LoadBlogContentPort loadBlogContentPort;
    private final LoadBlogSeoPort loadBlogSeoPort;
    private final BlogContentInfoAssembler contentInfoAssembler;
    private final BlogPolicyAuthorizationService policyAuthorizationService;

    @Override
    public BlogContentCursorInfo getPublicContents(BlogContentListQuery query) {
        BlogContentType type = parseOptionalType(query.type());
        BlogContentSort sort = BlogContentSort.from(query.sort());
        int size = normalizeSize(query.size());
        List<BlogContent> rows = loadBlogContentPort.listPublicContents(
            type,
            query.seriesSlug(),
            query.hashtagSlug(),
            sort,
            query.cursor(),
            size + 1
        );
        boolean hasNext = rows.size() > size;
        List<BlogContent> page = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new BlogContentCursorInfo(
            contentInfoAssembler.assembleSummaries(page, query.viewerMemberId(), isSuperAdmin(query.viewerMemberId())),
            nextCursor,
            hasNext
        );
    }

    @Override
    public BlogContentInfo getPublicContent(String typeValue, String slug, Long viewerMemberId) {
        BlogContentType type = BlogContentType.fromPath(typeValue);
        BlogContent content = loadBlogContentPort.findPublishedByTypeAndSlug(type, slug)
            .orElseThrow(() -> new BlogDomainException(BlogErrorCode.CONTENT_NOT_FOUND));
        return contentInfoAssembler.assemble(content, viewerMemberId, isSuperAdmin(viewerMemberId));
    }

    @Override
    public BlogContentInfo getPreview(Long contentId, Long viewerMemberId) {
        BlogContent content = loadBlogContentPort.findContentById(contentId)
            .filter(candidate -> !candidate.isDeleted())
            .orElseThrow(() -> new BlogDomainException(BlogErrorCode.CONTENT_NOT_FOUND));
        return contentInfoAssembler.assemble(content, viewerMemberId, isSuperAdmin(viewerMemberId));
    }

    @Override
    public BlogSeoPathsInfo getSeoPaths() {
        return new BlogSeoPathsInfo(loadBlogSeoPort.listPublicSeoPaths().stream()
            .map(row -> new BlogSeoPathInfo(row.type(), row.path(), row.updatedAt()))
            .toList());
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
