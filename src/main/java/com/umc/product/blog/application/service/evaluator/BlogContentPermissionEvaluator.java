package com.umc.product.blog.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.blog.application.authorization.BlogPolicyAction;
import com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.domain.BlogContent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlogContentPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadBlogContentPort loadBlogContentPort;
    private final BlogPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.BLOG_CONTENT;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        PermissionType permission = resourcePermission.permission();
        Long contentId = resourcePermission.getResourceIdAsLong();

        if (permission == PermissionType.WRITE && contentId == null) {
            return policyAuthorizationService.evaluate(
                BlogPolicyAction.CONTENT_CREATE,
                subjectAttributes,
                false);
        }
        if (contentId == null) {
            return false;
        }

        BlogContent content = loadBlogContentPort.findContentById(contentId).orElse(null);
        if (content == null || content.isDeleted()) {
            return false;
        }

        return switch (permission) {
            case READ -> evaluate(
                BlogPolicyAction.CONTENT_READ,
                subjectAttributes,
                content);
            case EDIT -> evaluate(
                BlogPolicyAction.CONTENT_UPDATE,
                subjectAttributes,
                content);
            case DELETE -> evaluate(
                BlogPolicyAction.CONTENT_DELETE,
                subjectAttributes,
                content);
            default -> {
                log.warn("BlogContentPermissionEvaluator에서 지원하지 않는 PermissionType: {}", permission);
                yield false;
            }
        };
    }

    private boolean isAuthor(Long memberId, BlogContent content) {
        return content.isAuthor(memberId);
    }

    private boolean evaluate(
        BlogPolicyAction action,
        SubjectAttributes subjectAttributes,
        BlogContent content
    ) {
        return policyAuthorizationService.evaluate(
            action,
            subjectAttributes,
            isAuthor(subjectAttributes.memberId(), content));
    }
}
