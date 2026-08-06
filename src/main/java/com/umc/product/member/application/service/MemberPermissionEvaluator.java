package com.umc.product.member.application.service;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MemberPermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.MEMBER;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case READ -> canReadMember(subjectAttributes, resourcePermission);
            case DELETE -> canDeleteMember(subjectAttributes);
            default ->
                throw new CommonException(CommonErrorCode.INTERNAL_SERVER_ERROR, "권한 확인 중 문제가 발생했어요. 관리자에게 문의해주세요.");
        };
    }

    // === PRIVATE METHODS ===

    private boolean canReadMember(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return !subjectAttributes.gisuChallengerInfos().isEmpty();
    }

    private boolean canDeleteMember(SubjectAttributes subjectAttributes) {
        // 회원 강제 삭제는 중앙운영사무국 총괄단만 가능합니다.
        return subjectAttributes.toAuthoritySnapshot().isCentralCoreInAnyGisu();
    }
}
