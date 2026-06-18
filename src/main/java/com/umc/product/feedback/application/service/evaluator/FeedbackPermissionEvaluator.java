package com.umc.product.feedback.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;

@Component
public class FeedbackPermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.FEEDBACK;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        if (resourcePermission.permission() != PermissionType.READ
            && resourcePermission.permission() != PermissionType.MANAGE) {
            return false;
        }
        return subjectAttributes.toAuthoritySnapshot().isSuperAdmin();
    }
}
