package com.umc.product.analytics.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@Component
public class AdminAnalyticsPermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.ANALYTICS;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        if (resourcePermission.permission() != PermissionType.READ) {
            return false;
        }

        if (subjectAttributes.toAuthoritySnapshot().isSuperAdmin()) {
            return true;
        }

        return subjectAttributes.roleAttributes().stream()
            .map(role -> role.roleType())
            .anyMatch(roleType -> roleType.isAtLeastCentralMember()
                || roleType == ChallengerRoleType.CHAPTER_PRESIDENT
                || roleType.isAtLeastSchoolAdmin());
    }
}
