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
public class ChallengerPermissionEvaluator implements ResourcePermissionEvaluator {

    private final ChallengerPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CHALLENGER;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case WRITE -> evaluate(ChallengerPolicyAction.CHALLENGER_CREATE, subjectAttributes);
            case EDIT -> evaluate(ChallengerPolicyAction.CHALLENGER_UPDATE, subjectAttributes);
            case DELETE -> evaluate(ChallengerPolicyAction.CHALLENGER_DELETE, subjectAttributes);
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
