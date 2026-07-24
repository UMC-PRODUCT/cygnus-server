package com.umc.product.organization.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.organization.application.authorization.OrganizationPolicyAction;
import com.umc.product.organization.application.authorization.OrganizationPolicyAuthorizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class StudyGroupPermissionEvaluator implements ResourcePermissionEvaluator {

    private final OrganizationPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.STUDY_GROUP;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case READ -> evaluate(OrganizationPolicyAction.STUDY_GROUP_READ, subjectAttributes);
            case WRITE -> evaluate(OrganizationPolicyAction.STUDY_GROUP_CREATE, subjectAttributes);
            case EDIT -> evaluate(OrganizationPolicyAction.STUDY_GROUP_UPDATE, subjectAttributes);
            case DELETE -> evaluate(OrganizationPolicyAction.STUDY_GROUP_DELETE, subjectAttributes);
            default -> throw new AuthorizationDomainException(AuthorizationErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED,
                "StudyGroupPermissionEvaluator에서 해당 PermissionType을 지원하지 않습니다: " + resourcePermission.permission());
        };
    }

    private boolean evaluate(
        OrganizationPolicyAction action,
        SubjectAttributes subjectAttributes
    ) {
        return policyAuthorizationService.evaluate(action, subjectAttributes);
    }
}
