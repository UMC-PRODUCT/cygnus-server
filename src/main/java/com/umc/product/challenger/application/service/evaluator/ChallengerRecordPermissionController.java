package com.umc.product.challenger.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.authorization.ChallengerPolicyAction;
import com.umc.product.challenger.application.authorization.ChallengerPolicyAuthorizationService;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChallengerRecordPermissionController implements ResourcePermissionEvaluator {

    private final ChallengerPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CHALLENGER_RECORD;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case READ -> evaluate(ChallengerPolicyAction.RECORD_READ, subjectAttributes);
            case WRITE -> evaluate(ChallengerPolicyAction.RECORD_CREATE, subjectAttributes);
            case DELETE -> evaluate(ChallengerPolicyAction.RECORD_DELETE, subjectAttributes);
            default -> throw new CommonException(CommonErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED); // 지원하지 않는 권한 유형은 거부
        };
    }

    private boolean evaluate(
        ChallengerPolicyAction action,
        SubjectAttributes subjectAttributes
    ) {
        return policyAuthorizationService.evaluate(action, subjectAttributes);
    }
}
