package com.umc.product.challenger.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

@Component
public class ChallengerRecordPermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CHALLENGER_RECORD;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case READ -> canRead(subjectAttributes);
            case MANAGE, WRITE, DELETE -> canManageOrWriteOrDelete(subjectAttributes);
            default -> throw new CommonException(CommonErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED); // 지원하지 않는 권한 유형은 거부
        };
    }

    private boolean canRead(SubjectAttributes subjectAttributes) {
        // 단건 조회(code/id)는 교내 회장/부회장 이상만 가능함
        return subjectAttributes.roleAttributes().stream()
            .anyMatch(roleAttribute -> roleAttribute.roleType().isAtLeastSchoolCore());
    }

    private boolean canManageOrWriteOrDelete(SubjectAttributes subjectAttributes) {
        // 코드 목록/통계 조회(MANAGE), 생성(WRITE), 삭제(DELETE)는 중앙운영사무국 총괄단 이상만 가능함
        return subjectAttributes.roleAttributes().stream()
            .anyMatch(roleAttribute -> roleAttribute.roleType().isAtLeastCentralCore());
    }
}
