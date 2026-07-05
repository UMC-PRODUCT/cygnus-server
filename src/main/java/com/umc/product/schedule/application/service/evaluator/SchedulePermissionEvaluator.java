package com.umc.product.schedule.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Schedule(일정) 리소스에 대한 권한 평가
 */
@Component
@RequiredArgsConstructor
public class SchedulePermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadSchedulePort loadSchedulePort;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.SCHEDULE;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {

        PermissionType permission = resourcePermission.permission();
        Long memberId = subjectAttributes.memberId();

        // READ (일정 조회), WRITE (일정 생성)
        // '챌린저 활동 기록이 있는 사용자'만 가능
        if (permission == PermissionType.READ || permission == PermissionType.WRITE) {
            // 챌린저 활동 기록이 있는 사용자인지 확인
            return !subjectAttributes.gisuChallengerInfos().isEmpty();
        }

        if (permission == PermissionType.EDIT || permission == PermissionType.DELETE) {

            // resourceId가 없으면 false 리턴
            if (resourcePermission.resourceId() == null) {
                return false;
            }

            Schedule schedule = loadSchedulePort.findById(resourcePermission.getResourceIdAsLong())
                .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

            if (isSuperAdmin(subjectAttributes)) {
                return true;
            }

            return schedule.getAuthorMemberId().equals(memberId);
        }

        if (permission == PermissionType.FORCE_DELETE) {

            if (resourcePermission.resourceId() == null) {
                return false;
            }

            Schedule schedule = loadSchedulePort.findById(resourcePermission.getResourceIdAsLong())
                .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

            return isSuperAdmin(subjectAttributes);
        }

        return false;
    }

    private boolean isSuperAdmin(SubjectAttributes subjectAttributes) {
        return subjectAttributes.toAuthoritySnapshot().isSuperAdmin();
    }
}
