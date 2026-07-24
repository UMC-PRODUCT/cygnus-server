package com.umc.product.member.application.service;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.member.application.authorization.MemberPolicyAction;
import com.umc.product.member.application.authorization.MemberPolicyAuthorizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class MemberPermissionEvaluator implements ResourcePermissionEvaluator {

    private final MemberPolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.MEMBER;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case READ -> policyAuthorizationService.evaluate(
                MemberPolicyAction.READ,
                subjectAttributes);
            case DELETE -> policyAuthorizationService.evaluate(
                MemberPolicyAction.DELETE,
                subjectAttributes);
            default ->
                throw new CommonException(CommonErrorCode.INTERNAL_SERVER_ERROR, "권한 확인 중 문제가 발생했어요. 관리자에게 문의해주세요.");
        };
    }
}
