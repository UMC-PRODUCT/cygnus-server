package com.umc.product.recruiting.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAction;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAuthorizationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingPermissionEvaluator implements ResourcePermissionEvaluator {

    private final RecruitingPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.RECRUITMENT;
    }

    @Override
    public boolean evaluate(
        SubjectAttributes subjectAttributes,
        ResourcePermission resourcePermission
    ) {
        RecruitingPolicyAction action = switch (resourcePermission.permission()) {
            case READ, WRITE, EDIT, APPROVE -> RecruitingPolicyAction.OPERATE_SCHOOL;
            case MANAGE -> RecruitingPolicyAction.MANAGE_ALL;
            default -> throw new AuthorizationDomainException(
                AuthorizationErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED,
                "RecruitingPermissionEvaluator에서 해당 PermissionType을 지원하지 않습니다: "
                    + resourcePermission.permission());
        };
        Long seasonId = resourcePermission.resourceId() == null
            ? null
            : resourcePermission.getResourceIdAsLong();
        return policyAuthorizationService.evaluateResource(
            action,
            subjectAttributes,
            seasonId);
    }
}
