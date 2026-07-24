package com.umc.product.community.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.community.application.authorization.CommunityPolicyAction;
import com.umc.product.community.application.authorization.CommunityPolicyAuthorizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommunityCommentPermissionEvaluator implements ResourcePermissionEvaluator {

    private final CommunityPolicyAuthorizationService policyAuthorizationService;

    /**
     * 이 Evaluator가 처리할 수 있는 ResourceType
     */
    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.COMMUNITY_COMMENT;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        CommunityPolicyAction action = switch (resourcePermission.permission()) {
            case READ -> CommunityPolicyAction.COMMENT_READ;
            case WRITE -> CommunityPolicyAction.COMMENT_WRITE;
            case EDIT -> CommunityPolicyAction.COMMENT_UPDATE;
            case DELETE -> CommunityPolicyAction.COMMENT_DELETE;
            default -> {
                log.warn("CommunityPostPE에서 지원하지 않는 PermissionType: {}", resourcePermission.permission());
                yield null;
            }
        };
        if (action == null) {
            return false;
        }
        return policyAuthorizationService.evaluateComment(
            action,
            subjectAttributes,
            resourcePermission.getResourceIdAsLong());
    }
}
