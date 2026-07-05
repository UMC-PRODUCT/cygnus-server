package com.umc.product.challenger.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

@Component
public class ChallengerPermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CHALLENGER;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case WRITE -> canCreateChallenger(subjectAttributes);
            case EDIT -> canUpdateChallenger(subjectAttributes, resourcePermission);
            case DELETE -> canDeleteChallenger(subjectAttributes);
            default -> throw new CommonException(CommonErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED); // 지원하지 않는 권한 유형은 거부
        };
    }

    private boolean canCreateChallenger(SubjectAttributes subjectAttributes) {
        // 교내 회장/부회장 이상만 가능함
        return subjectAttributes.toAuthoritySnapshot().isSuperAdmin()
            || subjectAttributes.roleAttributes().stream()
            .anyMatch(roleAttribute -> roleAttribute.roleType().isAtLeastSchoolCore());
    }

    private boolean canUpdateChallenger(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        // 중앙운영사무국 총괄단만 가능
        return subjectAttributes.toAuthoritySnapshot().isCentralCoreInAnyGisu();
    }

    private boolean canDeleteChallenger(SubjectAttributes subjectAttributes) {
        // 중앙운영사무국 총괄단만 가능함
        return subjectAttributes.toAuthoritySnapshot().isCentralCoreInAnyGisu();
    }
}
