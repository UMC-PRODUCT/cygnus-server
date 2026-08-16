package com.umc.product.curriculum.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookMissionUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditOriginalWorkbookMissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateOriginalWorkbookMissionCommand;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.SaveOriginalWorkbookMissionPort;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OriginalWorkbookMissionCommandService implements ManageOriginalWorkbookMissionUseCase {

    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final SaveOriginalWorkbookMissionPort saveOriginalWorkbookMissionPort;
    private final LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;

    @Override
    public Long create(CreateOriginalWorkbookMissionCommand command) {
        OriginalWorkbook workbook = loadOriginalWorkbookPort.getById(command.originalWorkbookId());
        boolean isNecessary = Boolean.TRUE.equals(command.isNecessary());

        if (workbook.getOriginalWorkbookStatus().isReleased() && isNecessary) {
            throw new CurriculumDomainException(CurriculumErrorCode.RELEASED_WORKBOOK_NECESSARY_MISSION_FORBIDDEN);
        }

        OriginalWorkbookMission mission = OriginalWorkbookMission.create(
            workbook,
            command.title(),
            command.description(),
            command.missionType(),
            isNecessary
        );

        return saveOriginalWorkbookMissionPort.save(mission).getId();
    }

    @Override
    public void edit(EditOriginalWorkbookMissionCommand command) {
        OriginalWorkbookMission mission = loadOriginalWorkbookMissionPort.getById(command.originalWorkbookMissionId());
        OriginalWorkbook workbook = mission.getOriginalWorkbook();

        if (command.isNecessary() != null
            && Boolean.TRUE.equals(command.isNecessary())
            && workbook.getOriginalWorkbookStatus().isReleased()
            && !mission.isNecessary()) {
            throw new CurriculumDomainException(CurriculumErrorCode.RELEASED_WORKBOOK_MISSION_UPGRADE_FORBIDDEN);
        }

        mission.edit(command.title(), command.description(), command.missionType(), command.isNecessary());
        saveOriginalWorkbookMissionPort.save(mission);
    }

    @Override
    public void delete(Long originalMissionId) {
        OriginalWorkbookMission mission = loadOriginalWorkbookMissionPort.getById(originalMissionId);

        if (loadMissionSubmissionPort.existsByOriginalWorkbookMissionId(originalMissionId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.MISSION_HAS_SUBMISSIONS);
        }

        saveOriginalWorkbookMissionPort.delete(mission);
    }
}
