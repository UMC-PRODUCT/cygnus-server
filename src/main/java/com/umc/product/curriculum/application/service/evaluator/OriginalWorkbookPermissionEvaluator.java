package com.umc.product.curriculum.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.curriculum.application.authorization.CurriculumPolicyAction;
import com.umc.product.curriculum.application.authorization.CurriculumPolicyAuthorizationService;
import com.umc.product.global.exception.constant.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OriginalWorkbookPermissionEvaluator implements ResourcePermissionEvaluator {

    private final CurriculumPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.ORIGINAL_WORKBOOK;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes,
                            ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case RELEASE -> policyAuthorizationService.evaluate(
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_RELEASE,
                subjectAttributes);
            case MANAGE -> policyAuthorizationService.evaluate(
                CurriculumPolicyAction.ORIGINAL_WORKBOOK_MANAGE,
                subjectAttributes);
            default -> throw new CommonException(CommonErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED);
        };
    }
}
