package com.umc.product.curriculum.application.service.evaluator;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumPermissionEvaluator implements ResourcePermissionEvaluator {

    private final LoadCurriculumPort loadCurriculumPort;
    private final LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    private final GetGisuUseCase getGisuUseCase;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.CURRICULUM;
    }

    @Override
    public boolean evaluate(SubjectAttributes subject, ResourcePermission permission) {
        if (permission.resourceId() == null
            || (permission.permission() != PermissionType.WRITE && permission.permission() != PermissionType.DELETE)) {
            return false;
        }
        try {
            Long gisuId = resolveGisuId(permission.resourceId());
            return subject.toAuthoritySnapshot().isCentralMemberInGisu(gisuId);
        } catch (BusinessException | NumberFormatException e) {
            return false;
        }
    }

    private Long resolveGisuId(String resourceId) {
        if (resourceId.startsWith("gisu:")) {
            return getGisuUseCase.getById(Long.valueOf(resourceId.substring(5))).gisuId();
        }
        if (resourceId.startsWith("weekly:")) {
            return loadWeeklyCurriculumPort.getById(Long.valueOf(resourceId.substring(7)))
                .getCurriculum().getGisuId();
        }
        return loadCurriculumPort.findById(Long.valueOf(resourceId))
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_NOT_FOUND))
            .getGisuId();
    }
}
