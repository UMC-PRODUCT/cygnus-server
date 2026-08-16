package com.umc.product.curriculum.application.service.evaluator;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.curriculum.application.port.out.LoadWeeklyBestWorkbookPort;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WeeklyBestWorkbookPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadWeeklyBestWorkbookPort loadWeeklyBestWorkbookPort;
    private final CurriculumStudyGroupStaffPolicy staffPolicy;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.WEEKLY_BEST_WORKBOOK;
    }

    @Override
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        Long resourceId = permission.getResourceIdAsLong();
        if (resourceId == null) {
            return false;
        }
        try {
            if (permission.permission() == PermissionType.WRITE) {
                return staffPolicy.canManageGroup(subject, resourceId);
            }
            WeeklyBestWorkbook best = loadWeeklyBestWorkbookPort.getById(resourceId);
            Long gisuId = best.getWeeklyCurriculum().getCurriculum().getGisuId();
            return staffPolicy.canManage(subject, best.getStudyGroupId(), best.getMemberId(), gisuId);
        } catch (BusinessException e) {
            return false;
        }
    }
}
