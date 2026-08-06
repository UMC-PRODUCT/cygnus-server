package com.umc.product.schedule.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.schedule.application.port.out.LoadScheduleParticipantPort;
import com.umc.product.schedule.application.port.out.LoadSchedulePort;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.exception.ScheduleDomainException;
import com.umc.product.schedule.domain.exception.ScheduleErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Attendance(출석) 리소스에 대한 권한 평가
 */
@Component
@RequiredArgsConstructor
public class AttendancePermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadSchedulePort loadSchedulePort;
    private final LoadScheduleParticipantPort loadScheduleParticipantPort;

    private final GetGisuUseCase getGisuUseCase;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.ATTENDANCE;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {

        PermissionType permission = resourcePermission.permission();
        Long memberId = subjectAttributes.memberId();

        // WRITE (출석 요청 / 사유 제출)
        // '챌린저 활동 기록이 있는 사용자'면서 '일정에 참여하는 사용자'만 가능
        if (permission == PermissionType.WRITE) {

            // resourceId가 없으면 false 리턴
            if (resourcePermission.resourceId() == null) {
                return false;
            }

            // 챌린저 활동 기록이 있는 사용자인지 확인
            if (subjectAttributes.gisuChallengerInfos().isEmpty()) {
                return false;
            }

            // 일정에 참여하는 사용자인지 확인
            return loadScheduleParticipantPort
                .findByScheduleIdAndMemberId(resourcePermission.getResourceIdAsLong(), memberId)
                .isPresent();
        }

        // APPROVE ([운영진용] 출석 승인 / 거절)
        // '해당 일정 기수의 운영진'만 가능
        if (permission == PermissionType.APPROVE) {

            // resourceId가 없으면 false 리턴
            if (resourcePermission.resourceId() == null) {
                return false;
            }

            // 해당 일정 기수의 운영진인지 확인
            return isTargetGisuAdmin(resourcePermission.getResourceIdAsLong(), subjectAttributes);
        }

        // READ ([운영진용] 일정 목록 / 단일일정 에 대한 출석 현황 조회)
        // 일정 목록 : '운영진 활동 이력이 있는 사용자'만 가능
        // 단일 일정 : '해당 일정 기수의 운영진'만 가능
        if (permission == PermissionType.READ) {

            // 일정 목록에 대한 출석 현황 조회 (보여주는 일정 목록 범위는 Service 단에서 필터링)
            // 운영진 활동 이력이 있는 사용자인지 확인
            if (resourcePermission.resourceId() == null) {
                return isAnyAdmin(subjectAttributes);
            }

            // 단일 일정에 대한 출석 현황 조회
            // 해당 일정 기수의 운영진인지 확인
            return isTargetGisuAdmin(resourcePermission.getResourceIdAsLong(), subjectAttributes);
        }

        return false;
    }

    // ============================= Helper Methods =============================


    // 특정 일정(scheduleId)이 진행되는 기수의 운영진인지 검사
    private boolean isTargetGisuAdmin(Long scheduleId, SubjectAttributes subjectAttributes) {
        if (subjectAttributes.toAuthoritySnapshot().isSuperAdmin()) {
            return true;
        }

        Schedule schedule = loadSchedulePort.findById(scheduleId)
            .orElseThrow(() -> new ScheduleDomainException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        Long targetGisuId = getGisuUseCase.getGisuByDate(schedule.getStartsAt()).gisuId();

        return subjectAttributes.roleAttributes().stream()
            .filter(role -> role.gisuId().equals(targetGisuId))
            .anyMatch(role -> isOperatingRole(role.roleType()));
    }


    // 사용자가 가지고 있는 역할 중 하나라도 운영진 역할이 있는지 검사
    private boolean isAnyAdmin(SubjectAttributes subjectAttributes) {
        return subjectAttributes.toAuthoritySnapshot().isSuperAdmin()
            || subjectAttributes.roleAttributes().stream()
            .anyMatch(role -> isOperatingRole(role.roleType()));
    }

    // 해당 권한이 출석을 관리할 수 있는 운영진 권한인지 판별
    private boolean isOperatingRole(ChallengerRoleType roleType) {
        return roleType.isAtLeastCentralMember()
            || roleType.isAtLeastSchoolAdmin();
    }
}
