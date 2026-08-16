package com.umc.product.notification.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;

@Component
public class FcmPermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.FCM;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        // 중앙운영사무국 총괄단 이상만 FCM 발송 요청과 토큰 삭제가 가능하도록 제한
        if (resourcePermission.permission().equals(PermissionType.WRITE)
            || resourcePermission.permission().equals(PermissionType.DELETE)) {
            return subjectAttributes.roleAttributes().stream()
                .anyMatch(roleAttribute -> roleAttribute.roleType().isAtLeastCentralCore());
        }

        return false;
    }
}
