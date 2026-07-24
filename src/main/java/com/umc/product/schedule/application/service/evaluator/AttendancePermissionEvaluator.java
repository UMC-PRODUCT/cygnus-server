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
 * Attendance(출석) 리소스에 대한 권한 평가
 */
@Component
@RequiredArgsConstructor
public class AttendancePermissionEvaluator implements ResourcePermissionEvaluator {

    private final SchedulePolicyAuthorizationService policyAuthorizationService;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.ATTENDANCE;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {

        SchedulePolicyAction action = switch (resourcePermission.permission()) {
            case WRITE -> SchedulePolicyAction.ATTENDANCE_SUBMIT;
            case READ -> SchedulePolicyAction.ATTENDANCE_READ;
            case APPROVE -> SchedulePolicyAction.ATTENDANCE_APPROVE;
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
