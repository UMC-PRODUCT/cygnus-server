package com.umc.product.analytics.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.analytics.application.authorization.AnalyticsPolicyAction;
import com.umc.product.analytics.application.authorization.AnalyticsPolicyAuthorizationService;
import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminAnalyticsPermissionEvaluator implements ResourcePermissionEvaluator {

    private final AnalyticsPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.ANALYTICS;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        if (resourcePermission.permission() != PermissionType.READ) {
            return false;
        }

        return policyAuthorizationService.evaluate(
            AnalyticsPolicyAction.READ_DASHBOARD,
            subjectAttributes);
    }
}
