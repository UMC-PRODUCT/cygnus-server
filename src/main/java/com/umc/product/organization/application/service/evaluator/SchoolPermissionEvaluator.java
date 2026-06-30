package com.umc.product.organization.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SchoolPermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.SCHOOL;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case WRITE, EDIT, DELETE -> subjectAttributes.toAuthoritySnapshot().isCentralCoreInAnyGisu();
            default -> throw new AuthorizationDomainException(AuthorizationErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED,
                "SchoolPermissionEvaluator에서 해당 PermissionType을 지원하지 않습니다: " + resourcePermission.permission());
        };
    }
}
