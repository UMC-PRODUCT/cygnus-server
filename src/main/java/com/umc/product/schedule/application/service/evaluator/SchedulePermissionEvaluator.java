package com.umc.product.schedule.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.schedule.application.authorization.SchedulePolicyAction;
import com.umc.product.schedule.application.authorization.SchedulePolicyAuthorizationService;

import lombok.RequiredArgsConstructor;

/**
 * Schedule(일정) 리소스에 대한 권한 평가
 */
@Component
@RequiredArgsConstructor
public class SchedulePermissionEvaluator implements ResourcePermissionEvaluator {

    private final SchedulePolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.SCHEDULE;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {

        SchedulePolicyAction action = switch (resourcePermission.permission()) {
            case READ -> SchedulePolicyAction.SCHEDULE_READ;
            case WRITE -> SchedulePolicyAction.SCHEDULE_CREATE;
            case EDIT -> SchedulePolicyAction.SCHEDULE_UPDATE;
            case DELETE -> SchedulePolicyAction.SCHEDULE_DELETE;
            case FORCE_DELETE -> SchedulePolicyAction.SCHEDULE_FORCE_DELETE;
            default -> null;
        };
        if (action == null) {
            return false;
        }
        Long scheduleId = resourcePermission.resourceId() == null
            ? null
            : resourcePermission.getResourceIdAsLong();
        return policyAuthorizationService.evaluate(action, subjectAttributes, scheduleId);
    }
}
