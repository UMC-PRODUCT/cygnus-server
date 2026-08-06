package com.umc.product.blog.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.domain.BlogContent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlogContentPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadBlogContentPort loadBlogContentPort;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.BLOG_CONTENT;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        PermissionType permission = resourcePermission.permission();
        Long contentId = resourcePermission.getResourceIdAsLong();

        if (permission == PermissionType.WRITE && contentId == null) {
            return isSuperAdmin(subjectAttributes);
        }
        if (contentId == null) {
            return false;
        }

        BlogContent content = loadBlogContentPort.findContentById(contentId).orElse(null);
        if (content == null || content.isDeleted()) {
            return false;
        }

        return switch (permission) {
            case READ -> isAuthor(subjectAttributes.memberId(), content) || isSuperAdmin(subjectAttributes);
            case EDIT -> isAuthor(subjectAttributes.memberId(), content);
            case DELETE -> isAuthor(subjectAttributes.memberId(), content) || isSuperAdmin(subjectAttributes);
            default -> {
                log.warn("BlogContentPermissionEvaluator에서 지원하지 않는 PermissionType: {}", permission);
                yield false;
            }
        };
    }

    private boolean isAuthor(Long memberId, BlogContent content) {
        return content.isAuthor(memberId);
    }

    private boolean isSuperAdmin(SubjectAttributes subjectAttributes) {
        return subjectAttributes.toAuthoritySnapshot().isSuperAdmin();
    }
}
