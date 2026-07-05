package com.umc.product.curriculum.application.service.evaluator;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.global.exception.constant.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionSubmissionPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.MISSION_SUBMISSION;
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
        Long challengerWorkbookId = resourcePermission.getResourceIdAsLong();
        if (challengerWorkbookId == null) {
            return false;
        }

        ChallengerWorkbook challengerWorkbook = loadChallengerWorkbookPort.getById(challengerWorkbookId);
        return Objects.equals(challengerWorkbook.getMemberId(), subjectAttributes.memberId());
    }

    private boolean canModify(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        Long missionSubmissionId = resourcePermission.getResourceIdAsLong();
        if (missionSubmissionId == null) {
            return false;
        }

        MissionSubmission missionSubmission = loadMissionSubmissionPort.getById(missionSubmissionId);
        return missionSubmission.isSubmittedBy(subjectAttributes.memberId());
    }
}
