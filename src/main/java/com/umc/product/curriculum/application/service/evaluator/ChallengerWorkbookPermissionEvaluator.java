package com.umc.product.curriculum.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChallengerWorkbookPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final CurriculumStudyGroupStaffPolicy staffPolicy;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CHALLENGER_WORKBOOK;
    }

    @Override
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        Long resourceId = permission.getResourceIdAsLong();
        if (resourceId == null) {
            return false;
        }
        try {
            ChallengerWorkbook workbook = loadChallengerWorkbookPort.getById(resourceId);
            Long gisuId = workbook.getOriginalWorkbook().getWeeklyCurriculum().getCurriculum().getGisuId();
            return staffPolicy.canManage(subject, workbook.getStudyGroupId(), workbook.getMemberId(), gisuId);
        } catch (BusinessException e) {
            return false;
        }
    }
}
