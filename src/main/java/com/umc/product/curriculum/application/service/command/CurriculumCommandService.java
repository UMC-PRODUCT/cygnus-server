package com.umc.product.curriculum.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.curriculum.application.port.in.command.ManageCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.CreateCurriculumCommand;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.EditCurriculumCommand;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveCurriculumPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CurriculumCommandService implements ManageCurriculumUseCase {

    private final LoadCurriculumPort loadCurriculumPort;
    private final SaveCurriculumPort saveCurriculumPort;
    private final LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    private final GetGisuUseCase getGisuUseCase;

    @Audited(
        domain = Domain.CURRICULUM,
        action = AuditAction.CREATE,
        targetType = "Curriculum",
        targetId = "#result",
        description = "'커리큘럼을 생성했습니다.'"
    )
    @Override
    public Long create(CreateCurriculumCommand command) {
        GisuLearningType learningType = getGisuUseCase.getById(command.gisuId()).learningType();
        boolean valid = learningType == GisuLearningType.PART
            ? command.part() != null && command.track() == null
            : command.part() == null && command.track() != null;
        if (!valid) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_CURRICULUM_LEARNING_TYPE);
        }
        Curriculum curriculum = learningType == GisuLearningType.PART
            ? Curriculum.create(command.gisuId(), command.part(), command.title())
            : Curriculum.createForTrack(command.gisuId(), command.track(), command.title());
        boolean exists = learningType == GisuLearningType.PART
            ? loadCurriculumPort.existsByGisuIdAndPart(command.gisuId(), command.part())
            : loadCurriculumPort.existsByGisuIdAndTrack(command.gisuId(), command.track());
        if (exists) {
            throw new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_ALREADY_EXISTS);
        }
        return saveCurriculumPort.save(curriculum).getId();
    }

    @Audited(
        domain = Domain.CURRICULUM,
        action = AuditAction.UPDATE,
        targetType = "Curriculum",
        targetId = "#command.curriculumId()",
        description = "'커리큘럼을 수정했습니다.'"
    )
    @Override
    public void edit(EditCurriculumCommand command) {
        Curriculum curriculum = loadCurriculumPort.findById(command.curriculumId())
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_NOT_FOUND));
        curriculum.updateTitle(command.title());
        saveCurriculumPort.save(curriculum);
    }

    @Audited(
        domain = Domain.CURRICULUM,
        action = AuditAction.DELETE,
        targetType = "Curriculum",
        targetId = "#curriculumId",
        description = "'커리큘럼을 삭제했습니다.'"
    )
    @Override
    public void delete(Long curriculumId) {
        Curriculum curriculum = loadCurriculumPort.findById(curriculumId)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_NOT_FOUND));
        if (loadWeeklyCurriculumPort.existsByCurriculumId(curriculumId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_HAS_WEEKLY_CURRICULUMS);
        }
        saveCurriculumPort.delete(curriculum);
    }
}
