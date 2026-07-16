package com.umc.product.blog.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.blog.application.port.in.query.GetBlogHashtagUseCase;
import com.umc.product.blog.application.port.in.query.dto.BlogContentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogHashtagCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogHashtagInfo;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogHashtagPort;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentSort;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.blog.domain.BlogDomainException;
import com.umc.product.blog.domain.BlogErrorCode;
import com.umc.product.blog.domain.BlogHashtag;
import com.umc.product.blog.domain.BlogHashtagSort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogHashtagQueryService implements GetBlogHashtagUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final LoadBlogHashtagPort loadBlogHashtagPort;
    private final LoadBlogContentPort loadBlogContentPort;
    private final BlogContentInfoAssembler contentInfoAssembler;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public BlogHashtagCursorInfo getPublicHashtags(String typeValue, String q, Long cursor, int requestedSize,
                                                   String sortValue) {
        BlogContentType type = parseOptionalType(typeValue);
        BlogHashtagSort sort = BlogHashtagSort.from(sortValue);
        int size = normalizeSize(requestedSize);
        List<BlogHashtag> rows = loadBlogHashtagPort.listPublicHashtags(type, q, sort, cursor, size + 1);
        boolean hasNext = rows.size() > size;
        List<BlogHashtag> page = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        Map<Long, Integer> counts = loadBlogHashtagPort.countPublishedContentsByHashtagIds(
            page.stream().map(BlogHashtag::getId).toList(),
            type
        );
        return new BlogHashtagCursorInfo(
            page.stream()
                .map(hashtag -> new BlogHashtagInfo(
                    hashtag.getId(),
                    hashtag.getName(),
                    hashtag.getSlug(),
                    counts.getOrDefault(hashtag.getId(), 0)
                ))
                .toList(),
            nextCursor,
            hasNext
        );
    }

    @Override
    public BlogContentCursorInfo getPublicHashtagContents(
        String hashtagSlug,
        String typeValue,
        Long cursor,
        int requestedSize,
        String sortValue,
        Long viewerMemberId
    ) {
        BlogHashtag hashtag = loadBlogHashtagPort.findBySlug(hashtagSlug)
            .orElseThrow(() -> new BlogDomainException(BlogErrorCode.HASHTAG_NOT_FOUND));
        BlogContentType type = parseOptionalType(typeValue);
        BlogContentSort sort = BlogContentSort.from(sortValue);
        int size = normalizeSize(requestedSize);
        List<BlogContent> rows = loadBlogContentPort.listPublicHashtagContents(hashtag.getId(), type, sort, cursor,
            size + 1);
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
        return memberId != null && getChallengerRoleUseCase.isSuperAdmin(memberId);
    }
}
