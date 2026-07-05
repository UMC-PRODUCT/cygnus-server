package com.umc.product.curriculum.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.global.exception.constant.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionFeedbackPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadMissionFeedbackPort loadMissionFeedbackPort;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.MISSION_FEEDBACK;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case WRITE -> canCreate(subjectAttributes, resourcePermission);
            case EDIT, DELETE -> canModify(subjectAttributes, resourcePermission);
            default -> throw new CommonException(CommonErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED);
        };
    }

    private boolean canCreate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        Long missionSubmissionId = resourcePermission.getResourceIdAsLong();
        if (missionSubmissionId == null) {
            return false;
        }

        return subjectAttributes.roleAttributes().stream()
            .anyMatch(roleAttribute -> roleAttribute.roleType().isAtLeastSchoolAdmin());
    }

    private boolean canModify(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        Long missionFeedbackId = resourcePermission.getResourceIdAsLong();
        if (missionFeedbackId == null) {
            return false;
        }

        MissionFeedback missionFeedback = loadMissionFeedbackPort.getById(missionFeedbackId);
        return missionFeedback.isReviewedBy(subjectAttributes.memberId());
    }
}
