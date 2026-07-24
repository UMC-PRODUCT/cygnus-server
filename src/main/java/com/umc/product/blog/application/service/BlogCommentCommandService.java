package com.umc.product.blog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService;
import com.umc.product.blog.application.port.in.command.CreateBlogCommentUseCase;
import com.umc.product.blog.application.port.in.command.DeleteBlogCommentUseCase;
import com.umc.product.blog.application.port.in.command.ToggleBlogCommentLikeUseCase;
import com.umc.product.blog.application.port.in.command.UpdateBlogCommentUseCase;
import com.umc.product.blog.application.port.in.command.dto.CreateBlogCommentCommand;
import com.umc.product.blog.application.port.in.command.dto.DeleteBlogCommentCommand;
import com.umc.product.blog.application.port.in.command.dto.UpdateBlogCommentCommand;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogLikeInfo;
import com.umc.product.blog.application.port.out.LoadBlogCommentPort;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogLikePort;
import com.umc.product.blog.application.port.out.SaveBlogCommentPort;
import com.umc.product.blog.application.port.out.SaveBlogLikePort;
import com.umc.product.blog.domain.BlogComment;
import com.umc.product.blog.domain.BlogCommentLike;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.blog.domain.BlogDomainException;
import com.umc.product.blog.domain.BlogErrorCode;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogCommentCommandService implements CreateBlogCommentUseCase, UpdateBlogCommentUseCase,
    DeleteBlogCommentUseCase, ToggleBlogCommentLikeUseCase {

    private final LoadBlogContentPort loadBlogContentPort;
    private final LoadBlogCommentPort loadBlogCommentPort;
    private final SaveBlogCommentPort saveBlogCommentPort;
    private final LoadBlogLikePort loadBlogLikePort;
    private final SaveBlogLikePort saveBlogLikePort;
    private final BlogCommentInfoAssembler commentInfoAssembler;
    private final BlogPolicyAuthorizationService policyAuthorizationService;

    @Audited(
        domain = Domain.BLOG,
        action = AuditAction.CREATE,
        targetType = "BlogComment",
        targetId = "#result.id()",
        description = "'블로그 댓글을 생성했습니다.'"
    )
    @Override
    public BlogCommentInfo create(CreateBlogCommentCommand command) {
        BlogContent content = getPublishedContent(command.type(), command.slug());
        validateParent(content.getId(), command.parentCommentId());

        BlogComment comment = BlogComment.create(
            content.getId(),
            command.parentCommentId(),
            command.authorMemberId(),
            command.anonymous(),
            command.nickname(),
            command.content()
        );
        BlogComment saved = saveBlogCommentPort.save(comment);
        return commentInfoAssembler.assemble(
            saved,
            false,
            0,
            List.of(),
            command.authorMemberId(),
            isSuperAdmin(command.authorMemberId())
        );
    }

    @Audited(
        domain = Domain.BLOG,
        action = AuditAction.UPDATE,
        targetType = "BlogComment",
        targetId = "#command.commentId()",
        description = "'블로그 댓글을 수정했습니다.'"
    )
    @Override
    public BlogCommentInfo update(UpdateBlogCommentCommand command) {
        BlogContent content = getPublishedContent(command.type(), command.slug());
        BlogComment comment = getCommentInContent(command.commentId(), content.getId());
        comment.updateContent(command.content());

        BlogComment updated = saveBlogCommentPort.save(comment);
        return assembleSingle(updated, command.memberId());
    }

    @Audited(
        domain = Domain.BLOG,
        action = AuditAction.DELETE,
        targetType = "BlogComment",
        targetId = "#command.commentId()",
        description = "'블로그 댓글을 삭제했습니다.'"
    )
    @Override
    public void delete(DeleteBlogCommentCommand command) {
        BlogContent content = getPublishedContent(command.type(), command.slug());
        BlogComment comment = getCommentInContent(command.commentId(), content.getId());
        comment.ensureNotDeleted();
        deleteResolved(comment, command.memberId(), isAdminDeletion(comment, command.memberId()));
    }

    @Override
    public BlogLikeInfo toggle(String type, String slug, Long commentId, Long memberId) {
        BlogContent content = getPublishedContent(type, slug);
        BlogComment comment = getCommentInContent(commentId, content.getId());
        comment.ensureNotDeleted();
        boolean liked = toggleCommentLike(comment.getId(), memberId);
        return new BlogLikeInfo(liked, loadBlogLikePort.countCommentLikes(comment.getId()));
    }

    private void validateParent(Long contentId, Long parentCommentId) {
        if (parentCommentId == null) {
            return;
        }

        BlogComment parent = getCommentInContent(parentCommentId, contentId);
        parent.ensureNotDeleted();
        if (parent.getParentCommentId() != null) {
            throw new BlogDomainException(BlogErrorCode.INVALID_PARENT_COMMENT);
        }
    }

    private void deleteResolved(BlogComment comment, Long memberId, boolean adminDeletion) {
        if (loadBlogCommentPort.existsVisibleReply(comment.getId())) {
            if (adminDeletion) {
                comment.deleteByAdmin(memberId);
            } else {
                comment.deleteByUser(memberId);
            }
            saveBlogCommentPort.save(comment);
            saveBlogLikePort.deleteCommentLikesByCommentId(comment.getId());
            return;
        }
        saveBlogCommentPort.hardDelete(comment.getId());
    }

    private boolean isAdminDeletion(BlogComment comment, Long memberId) {
        return !isAuthor(comment, memberId) && isSuperAdmin(memberId);
    }

    private boolean isAuthor(BlogComment comment, Long memberId) {
        return memberId != null
            && comment.getAuthorMemberId() != null
            && memberId.equals(comment.getAuthorMemberId());
    }

    private BlogCommentInfo assembleSingle(BlogComment comment, Long viewerMemberId) {
        List<Long> commentIds = List.of(comment.getId());
        int likeCount = loadBlogLikePort.countCommentLikesByCommentIds(commentIds)
            .getOrDefault(comment.getId(), 0);
        boolean likedByMe = loadBlogLikePort.findLikedCommentIds(commentIds, viewerMemberId)
            .contains(comment.getId());

        return commentInfoAssembler.assemble(
            comment,
            likedByMe,
            likeCount,
            List.of(),
            viewerMemberId,
            isSuperAdmin(viewerMemberId)
        );
    }

    private boolean isSuperAdmin(Long memberId) {
        return policyAuthorizationService.isSuperAdminViewer(memberId);
    }

    private BlogContent getPublishedContent(String typeValue, String slug) {
        BlogContentType type = BlogContentType.fromPath(typeValue);
        return loadBlogContentPort.findPublishedByTypeAndSlug(type, slug)
            .orElseThrow(() -> new BlogDomainException(BlogErrorCode.CONTENT_NOT_PUBLISHED));
    }

    private BlogComment getCommentInContent(Long commentId, Long contentId) {
        return loadBlogCommentPort.getByIdAndContentId(commentId, contentId);
    }

    private boolean toggleCommentLike(Long commentId, Long memberId) {
        if (loadBlogLikePort.existsCommentLike(commentId, memberId)) {
            saveBlogLikePort.deleteCommentLike(commentId, memberId);
            return false;
        }
        saveBlogLikePort.saveCommentLike(BlogCommentLike.create(commentId, memberId));
        return true;
    }
}
