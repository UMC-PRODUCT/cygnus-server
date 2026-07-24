package com.umc.product.blog.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService;
import com.umc.product.blog.application.port.in.query.GetBlogCommentListUseCase;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentListQuery;
import com.umc.product.blog.application.port.out.LoadBlogCommentPort;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogLikePort;
import com.umc.product.blog.domain.BlogComment;
import com.umc.product.blog.domain.BlogCommentSort;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogCommentQueryService implements GetBlogCommentListUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final LoadBlogContentPort loadBlogContentPort;
    private final LoadBlogCommentPort loadBlogCommentPort;
    private final LoadBlogLikePort loadBlogLikePort;
    private final BlogCommentInfoAssembler commentInfoAssembler;
    private final BlogPolicyAuthorizationService policyAuthorizationService;

    @Override
    public BlogCommentCursorInfo getComments(BlogCommentListQuery query) {
        BlogContentType type = BlogContentType.fromPath(query.type());
        return loadBlogContentPort.findPublishedByTypeAndSlug(type, query.slug())
            .map(content -> getComments(content, query))
            .orElseGet(() -> new BlogCommentCursorInfo(List.of(), null, false));
    }

    private BlogCommentCursorInfo getComments(BlogContent content, BlogCommentListQuery query) {
        int size = normalizeSize(query.size());
        BlogCommentSort sort = BlogCommentSort.from(query.sort());

        List<BlogComment> rows = loadBlogCommentPort.listTopLevel(
            content.getId(),
            sort,
            query.cursor(),
            size + 1
        );

        boolean hasNext = rows.size() > size;
        List<BlogComment> page = hasNext ? rows.subList(0, size) : rows;
        if (page.isEmpty()) {
            return new BlogCommentCursorInfo(List.of(), null, false);
        }

        List<Long> parentIds = page.stream().map(BlogComment::getId).toList();
        Map<Long, List<BlogComment>> repliesByParentId = loadBlogCommentPort
            .listRepliesByParentIds(parentIds)
            .stream()
            .collect(Collectors.groupingBy(BlogComment::getParentCommentId));

        List<Long> allCommentIds = page.stream()
            .flatMap(comment -> {
                List<BlogComment> replies = repliesByParentId.getOrDefault(comment.getId(), List.of());
                return java.util.stream.Stream.concat(java.util.stream.Stream.of(comment), replies.stream());
            })
            .map(BlogComment::getId)
            .toList();

        Map<Long, Integer> likeCounts = loadBlogLikePort.countCommentLikesByCommentIds(allCommentIds);
        Set<Long> likedCommentIds = loadBlogLikePort.findLikedCommentIds(allCommentIds, query.viewerMemberId());
        List<BlogCommentInfo> comments = commentInfoAssembler.assembleTree(
            page,
            repliesByParentId,
            likeCounts,
            likedCommentIds,
            query.viewerMemberId(),
            isSuperAdmin(query.viewerMemberId())
        );

        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
        return new BlogCommentCursorInfo(comments, nextCursor, hasNext);
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
