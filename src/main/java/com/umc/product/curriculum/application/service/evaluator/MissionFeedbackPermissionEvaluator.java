package com.umc.product.curriculum.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionFeedbackPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final LoadMissionFeedbackPort loadMissionFeedbackPort;
    private final CurriculumStudyGroupStaffPolicy staffPolicy;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.MISSION_FEEDBACK;
    }

    @Override
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        Long resourceId = permission.getResourceIdAsLong();
        if (resourceId == null) {
            return false;
        }
        try {
            if (permission.permission() == PermissionType.WRITE) {
                return canManageSubmission(subject, loadMissionSubmissionPort.getById(resourceId));
            }
            MissionFeedback feedback = loadMissionFeedbackPort.getById(resourceId);
            return feedback.isReviewedBy(subject.memberId());
        } catch (BusinessException e) {
            return false;
        }
    }

    private boolean canManageSubmission(SubjectAttributes subject, MissionSubmission submission) {
        ChallengerWorkbook workbook = submission.getChallengerWorkbook();
        Long gisuId = workbook.getOriginalWorkbook().getWeeklyCurriculum().getCurriculum().getGisuId();
        return staffPolicy.canManage(subject, workbook.getStudyGroupId(), workbook.getMemberId(), gisuId);
    }
}
