package com.umc.product.blog.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.blog.application.authorization.BlogPolicyAction;
import com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService;
import com.umc.product.blog.application.port.out.LoadBlogCommentPort;
import com.umc.product.blog.domain.BlogComment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlogCommentPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadBlogCommentPort loadBlogCommentPort;
    private final BlogPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.BLOG_COMMENT;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        PermissionType permission = resourcePermission.permission();
        Long commentId = resourcePermission.getResourceIdAsLong();
        if (commentId == null) {
            return false;
        }

        BlogComment comment = loadBlogCommentPort.findById(commentId).orElse(null);
        if (comment == null) {
            return false;
        }

        return switch (permission) {
            case EDIT -> evaluate(
                BlogPolicyAction.COMMENT_UPDATE,
                subjectAttributes,
                comment);
            case DELETE -> evaluate(
                BlogPolicyAction.COMMENT_DELETE,
                subjectAttributes,
                comment);
            default -> {
                log.warn("BlogCommentPermissionEvaluator에서 지원하지 않는 PermissionType: {}", permission);
                yield false;
            }
        };
    }

    private boolean isAuthor(Long memberId, BlogComment comment) {
        return memberId != null
            && comment.getAuthorMemberId() != null
            && comment.getAuthorMemberId().equals(memberId);
    }

    private boolean evaluate(
        BlogPolicyAction action,
        SubjectAttributes subjectAttributes,
        BlogComment comment
    ) {
        return policyAuthorizationService.evaluate(
            action,
            subjectAttributes,
            isAuthor(subjectAttributes.memberId(), comment));
    }
}
