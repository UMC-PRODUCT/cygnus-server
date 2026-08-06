package com.umc.product.blog.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.blog.application.port.out.LoadBlogSeriesPort;
import com.umc.product.blog.domain.BlogSeries;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlogSeriesPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadBlogSeriesPort loadBlogSeriesPort;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.BLOG_SERIES;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        PermissionType permission = resourcePermission.permission();
        Long seriesId = resourcePermission.getResourceIdAsLong();

        if (permission == PermissionType.WRITE && seriesId == null) {
            return isSuperAdmin(subjectAttributes);
        }
        if (seriesId == null) {
            return false;
        }

        BlogSeries series = loadBlogSeriesPort.findSeriesById(seriesId).orElse(null);
        if (series == null || series.isDeleted()) {
            return false;
        }

        return switch (permission) {
            case READ -> isAuthor(subjectAttributes.memberId(), series) || isSuperAdmin(subjectAttributes);
            case EDIT -> isAuthor(subjectAttributes.memberId(), series);
            case DELETE -> isAuthor(subjectAttributes.memberId(), series) || isSuperAdmin(subjectAttributes);
            default -> {
                log.warn("BlogSeriesPermissionEvaluator에서 지원하지 않는 PermissionType: {}", permission);
                yield false;
            }
        };
    }

    private boolean isAuthor(Long memberId, BlogSeries series) {
        return series.isAuthor(memberId);
    }

    private boolean isSuperAdmin(SubjectAttributes subjectAttributes) {
        return subjectAttributes.toAuthoritySnapshot().isSuperAdmin();
    }
}
