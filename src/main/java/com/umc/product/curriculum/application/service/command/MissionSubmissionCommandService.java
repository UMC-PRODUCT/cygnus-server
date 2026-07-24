package com.umc.product.curriculum.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.curriculum.application.port.in.command.ManageMissionSubmissionUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.DeleteMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.SaveMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.SaveMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionSubmissionCommandService implements ManageMissionSubmissionUseCase {

    private final LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    private final LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final SaveMissionSubmissionPort saveMissionSubmissionPort;
    private final SaveMissionFeedbackPort saveMissionFeedbackPort;
    private final MissionMutationPolicy missionMutationPolicy;

    @Audited(
        domain = Domain.CURRICULUM,
        action = AuditAction.SUBMIT,
        targetType = "MissionSubmission",
        targetId = "#result",
        description = "'미션 제출물을 생성했습니다.'"
    )
    @Override
    public Long create(CreateMissionSubmissionCommand command) {
        OriginalWorkbookMission mission = loadOriginalWorkbookMissionPort.getById(
            command.originalWorkbookMissionId()
        );
        ChallengerWorkbook workbook = loadChallengerWorkbookPort.getById(command.challengerWorkbookId());

        validateOwner(workbook, command.requesterMemberId());
        validateMissionBelongsToWorkbook(mission, workbook);
        missionMutationPolicy.validateSubmissionCreate(mission.getOriginalWorkbook().getWeeklyCurriculum());
        validateNotSubmitted(command.originalWorkbookMissionId(), command.challengerWorkbookId());

        return saveMissionSubmissionPort.save(MissionSubmission.create(mission, workbook, command.content())).getId();
    }

    @Override
    public void edit(EditMissionSubmissionCommand command) {
        MissionSubmission submission = loadMissionSubmissionPort.getByIdForUpdate(command.missionSubmissionId());
        validateSubmissionOwner(submission, command.requesterMemberId());
        missionMutationPolicy.validateSubmissionEdit(submission);
        submission.edit(command.content());
        saveMissionSubmissionPort.save(submission);
    }

    @Override
    public void withdraw(DeleteMissionSubmissionCommand command) {
        MissionSubmission submission = loadMissionSubmissionPort.getByIdForUpdate(command.missionSubmissionId());
        validateSubmissionOwner(submission, command.requesterMemberId());
        submission.withdraw(missionMutationPolicy.now());
        saveMissionFeedbackPort.deleteByMissionSubmissionId(command.missionSubmissionId());
        saveMissionSubmissionPort.save(submission);
    }

    private void validateOwner(ChallengerWorkbook workbook, Long requesterMemberId) {
        if (!workbook.isOwnedBy(requesterMemberId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }

    private void validateSubmissionOwner(MissionSubmission submission, Long requesterMemberId) {
        if (!submission.isSubmittedBy(requesterMemberId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }

    private void validateMissionBelongsToWorkbook(OriginalWorkbookMission mission, ChallengerWorkbook workbook) {
        if (!Objects.equals(mission.getOriginalWorkbook().getId(), workbook.getOriginalWorkbook().getId())) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_NOT_IN_CURRICULUM);
        }
    }

    private void validateNotSubmitted(Long missionId, Long workbookId) {
        if (loadMissionSubmissionPort.existsByOriginalWorkbookMissionIdAndChallengerWorkbookId(
            missionId, workbookId
        )) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_SUBMISSION_ALREADY_EXISTS);
        }
    }
}
