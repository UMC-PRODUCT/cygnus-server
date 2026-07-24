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
public class CommunityPostPermissionEvaluator implements ResourcePermissionEvaluator {

    private final CommunityPolicyAuthorizationService policyAuthorizationService;

    /**
     * 이 Evaluator가 처리할 수 있는 ResourceType
     */
    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.COMMUNITY_POST;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        CommunityPolicyAction action = switch (resourcePermission.permission()) {
            case READ -> CommunityPolicyAction.POST_READ;
            case WRITE -> CommunityPolicyAction.POST_WRITE;
            case EDIT -> CommunityPolicyAction.POST_UPDATE;
            case DELETE -> CommunityPolicyAction.POST_DELETE;
            default -> {
                log.warn("CommunityPermissionEvaluator에서 지원하지 않는 PermissionType: {}", resourcePermission.permission());
                yield null;
            }
        };
        if (action == null) {
            return false;
        }
        return policyAuthorizationService.evaluatePost(
            action,
            subjectAttributes,
            resourcePermission.getResourceIdAsLong());
    }
}
