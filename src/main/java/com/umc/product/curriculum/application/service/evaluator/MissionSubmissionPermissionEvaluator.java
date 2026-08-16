package com.umc.product.curriculum.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.global.exception.BusinessException;

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
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        Long resourceId = permission.getResourceIdAsLong();
        if (resourceId == null) {
            return false;
        }
        try {
            if (permission.permission() == PermissionType.WRITE) {
                return loadChallengerWorkbookPort.getById(resourceId)
                    .isOwnedBy(subject.memberId());
            }
            MissionSubmission submission = loadMissionSubmissionPort.getById(resourceId);
            return submission.isSubmittedBy(subject.memberId());
        } catch (BusinessException e) {
            return false;
        }
    }
}
