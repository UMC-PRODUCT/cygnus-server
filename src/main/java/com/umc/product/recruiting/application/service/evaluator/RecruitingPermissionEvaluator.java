package com.umc.product.recruiting.application.service.evaluator;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingSeason;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadRecruitingSeasonPort loadRecruitingSeasonPort;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.RECRUITMENT;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case READ, WRITE, EDIT, APPROVE -> canOperateSchoolRecruiting(subjectAttributes, resourcePermission);
            case MANAGE -> canManageAllRecruiting(subjectAttributes, resourcePermission);
            default -> throw new AuthorizationDomainException(
                AuthorizationErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED,
                "RecruitingPermissionEvaluator에서 해당 PermissionType을 지원하지 않습니다: "
                    + resourcePermission.permission()
            );
        };
    }

    private boolean canOperateSchoolRecruiting(
        SubjectAttributes subjectAttributes,
        ResourcePermission resourcePermission
    ) {
        if (resourcePermission.resourceId() == null) {
            return hasAnyRecruitingOperatorRole(subjectAttributes);
        }

        RecruitingSeason season = loadRecruitingSeason(resourcePermission);
        return isCentralCoreInGisu(subjectAttributes, season.getGisuId())
            || isSchoolCoreOf(subjectAttributes, season.getGisuId(), season.getSchoolId());
    }

    private boolean canManageAllRecruiting(
        SubjectAttributes subjectAttributes,
        ResourcePermission resourcePermission
    ) {
        if (resourcePermission.resourceId() == null) {
            return hasAnyCentralCoreRole(subjectAttributes);
        }

        RecruitingSeason season = loadRecruitingSeason(resourcePermission);
        return isCentralCoreInGisu(subjectAttributes, season.getGisuId());
    }

    private RecruitingSeason loadRecruitingSeason(ResourcePermission resourcePermission) {
        return loadRecruitingSeasonPort.getById(resourcePermission.getResourceIdAsLong());
    }

    private boolean hasAnyRecruitingOperatorRole(SubjectAttributes subjectAttributes) {
        return subjectAttributes.roleAttributes().stream()
            .anyMatch(roleAttribute -> roleAttribute.roleType().isAtLeastCentralCore()
                || roleAttribute.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || roleAttribute.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT);
    }

    private boolean hasAnyCentralCoreRole(SubjectAttributes subjectAttributes) {
        return subjectAttributes.roleAttributes().stream()
            .map(RoleAttribute::roleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralCore);
    }

    private boolean isCentralCoreInGisu(SubjectAttributes subjectAttributes, Long gisuId) {
        return subjectAttributes.roleAttributes().stream()
            .anyMatch(roleAttribute -> roleAttribute.roleType().isSuperAdmin()
                || (roleAttribute.roleType().isAtLeastCentralCore()
                && Objects.equals(roleAttribute.gisuId(), gisuId)));
    }

    private boolean isSchoolCoreOf(SubjectAttributes subjectAttributes, Long gisuId, Long schoolId) {
        return subjectAttributes.roleAttributes().stream()
            .anyMatch(roleAttribute -> isSchoolCore(roleAttribute.roleType())
                && Objects.equals(roleAttribute.gisuId(), gisuId)
                && Objects.equals(roleAttribute.organizationId(), schoolId));
    }

    private boolean isSchoolCore(ChallengerRoleType roleType) {
        return roleType == ChallengerRoleType.SCHOOL_PRESIDENT
            || roleType == ChallengerRoleType.SCHOOL_VICE_PRESIDENT;
    }
}
