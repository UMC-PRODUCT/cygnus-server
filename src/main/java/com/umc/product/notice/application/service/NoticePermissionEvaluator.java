package com.umc.product.notice.application.service;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.notice.application.authorization.NoticePolicyAction;
import com.umc.product.notice.application.authorization.NoticePolicyAuthorizationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticePermissionEvaluator implements ResourcePermissionEvaluator {

    private final NoticePolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.NOTICE;
    }

    @Override
    public boolean evaluate(
        SubjectAttributes subjectAttributes,
        ResourcePermission resourcePermission
    ) {
        if (!resourcePermission.resourceType().getSupportedPermissions()
            .contains(resourcePermission.permission())) {
            throw new AuthorizationDomainException(
                AuthorizationErrorCode.INVALID_RESOURCE_PERMISSION_GIVEN,
                "NoticePermissionEvaluator에서 지원하지 않는 권한 유형에 대한 평가가 시도되었습니다: "
                    + resourcePermission.permission());
        }
        NoticePolicyAction action = switch (resourcePermission.permission()) {
            case READ -> NoticePolicyAction.READ;
            case EDIT -> NoticePolicyAction.UPDATE;
            case DELETE -> NoticePolicyAction.DELETE;
            case MANAGE -> NoticePolicyAction.READ_RECIPIENTS;
            case CHECK -> NoticePolicyAction.CHECK_RECIPIENTS;
            default -> throw new AuthorizationDomainException(
                AuthorizationErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED,
                "NoticePermissionEvaluator에서 지원하지 않는 권한 유형입니다: "
                    + resourcePermission.permission());
        };
        return policyAuthorizationService.evaluateResource(
            action,
            subjectAttributes,
            resourcePermission.getResourceIdAsLong());
    }
}
