package com.umc.product.blog.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.blog.application.authorization.BlogPolicyAction;
import com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService;
import com.umc.product.blog.application.port.out.LoadBlogSeriesPort;
import com.umc.product.blog.domain.BlogSeries;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlogSeriesPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadBlogSeriesPort loadBlogSeriesPort;
    private final BlogPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.BLOG_SERIES;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        PermissionType permission = resourcePermission.permission();
        Long seriesId = resourcePermission.getResourceIdAsLong();

        if (permission == PermissionType.WRITE && seriesId == null) {
            return policyAuthorizationService.evaluate(
                BlogPolicyAction.SERIES_CREATE,
                subjectAttributes,
                false);
        }
        if (seriesId == null) {
            return false;
        }

        BlogSeries series = loadBlogSeriesPort.findSeriesById(seriesId).orElse(null);
        if (series == null || series.isDeleted()) {
            return false;
        }

        return switch (permission) {
            case READ -> evaluate(
                BlogPolicyAction.SERIES_READ,
                subjectAttributes,
                series);
            case EDIT -> evaluate(
                BlogPolicyAction.SERIES_UPDATE,
                subjectAttributes,
                series);
            case DELETE -> evaluate(
                BlogPolicyAction.SERIES_DELETE,
                subjectAttributes,
                series);
            default -> {
                log.warn("BlogSeriesPermissionEvaluator에서 지원하지 않는 PermissionType: {}", permission);
                yield false;
            }
        };
    }

    private boolean isAuthor(Long memberId, BlogSeries series) {
        return series.isAuthor(memberId);
    }

    private boolean evaluate(
        BlogPolicyAction action,
        SubjectAttributes subjectAttributes,
        BlogSeries series
    ) {
        return policyAuthorizationService.evaluate(
            action,
            subjectAttributes,
            isAuthor(subjectAttributes.memberId(), series));
    }
}
