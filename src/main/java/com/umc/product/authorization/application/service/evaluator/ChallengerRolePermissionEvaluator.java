package com.umc.product.authorization.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;

@Component
public class ChallengerRolePermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CHALLENGER_ROLE;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        // 조회는 누구나 가능하도록 구현
        if (resourcePermission.permission().equals(PermissionType.READ)) {
            return true;
        }

        // 조회를 제외한 생성/수정/삭제는 중앙운영사무국 총괄만 가능하도록 설정함. 추후 개선
        // TODO: 중앙운영사무국 측과 협의할 것
        return subjectAttributes.toAuthoritySnapshot().isCentralCoreInAnyGisu();
    }
}
