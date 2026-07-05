package com.umc.product.curriculum.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.curriculum.application.port.in.command.ManageMissionSubmissionUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.WithdrawMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.SaveMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.SaveMissionSubmissionPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
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

    @Audited(
        domain = Domain.CURRICULUM,
        action = AuditAction.SUBMIT,
        targetType = "MissionSubmission",
        targetId = "#result",
        description = "'미션 제출물을 생성했습니다.'"
    )
    @Override
    public Long create(CreateMissionSubmissionCommand command) {
        OriginalWorkbookMission originalWorkbookMission =
            loadOriginalWorkbookMissionPort.getById(command.originalWorkbookMissionId());
        ChallengerWorkbook challengerWorkbook = loadChallengerWorkbookPort.getById(command.challengerWorkbookId());

        validateRequesterOwnsWorkbook(challengerWorkbook, command.requesterMemberId());
        validateMissionBelongsToWorkbook(originalWorkbookMission, challengerWorkbook);
        validateNotSubmitted(command.originalWorkbookMissionId(), command.challengerWorkbookId());

        MissionSubmission missionSubmission = MissionSubmission.create(
            originalWorkbookMission,
            challengerWorkbook,
            command.content()
        );

        return saveMissionSubmissionPort.save(missionSubmission).getId();
    }

    @Override
    public void edit(EditMissionSubmissionCommand command) {
        MissionSubmission missionSubmission = loadMissionSubmissionPort.getById(command.missionSubmissionId());
        validateRequesterOwnsSubmission(missionSubmission, command.requesterMemberId());

        missionSubmission.edit(command.content());
        saveMissionSubmissionPort.save(missionSubmission);
    }

    @Override
    public void withdraw(WithdrawMissionSubmissionCommand command) {
        MissionSubmission missionSubmission = loadMissionSubmissionPort.getById(command.missionSubmissionId());
        validateRequesterOwnsSubmission(missionSubmission, command.requesterMemberId());

        saveMissionFeedbackPort.deleteByMissionSubmissionId(command.missionSubmissionId());
        saveMissionSubmissionPort.delete(missionSubmission);
    }

    private void validateRequesterOwnsWorkbook(ChallengerWorkbook challengerWorkbook, Long requesterMemberId) {
        if (!Objects.equals(challengerWorkbook.getMemberId(), requesterMemberId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }

    private void validateRequesterOwnsSubmission(MissionSubmission missionSubmission, Long requesterMemberId) {
        if (!missionSubmission.isSubmittedBy(requesterMemberId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }

    private void validateMissionBelongsToWorkbook(
        OriginalWorkbookMission originalWorkbookMission,
        ChallengerWorkbook challengerWorkbook
    ) {
        if (!isSameOriginalWorkbook(originalWorkbookMission, challengerWorkbook)) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_NOT_IN_CURRICULUM);
        }
    }

    private boolean isSameOriginalWorkbook(
        OriginalWorkbookMission originalWorkbookMission,
        ChallengerWorkbook challengerWorkbook
    ) {
        OriginalWorkbook missionWorkbook = originalWorkbookMission.getOriginalWorkbook();
        OriginalWorkbook challengerWorkbookOriginal = challengerWorkbook.getOriginalWorkbook();

        Long missionWorkbookId = missionWorkbook.getId();
        Long challengerWorkbookOriginalId = challengerWorkbookOriginal.getId();
        if (missionWorkbookId != null && challengerWorkbookOriginalId != null) {
            return Objects.equals(missionWorkbookId, challengerWorkbookOriginalId);
        }

        return missionWorkbook == challengerWorkbookOriginal;
    }

    private void validateNotSubmitted(Long originalWorkbookMissionId, Long challengerWorkbookId) {
        if (loadMissionSubmissionPort.existsByOriginalWorkbookMissionIdAndChallengerWorkbookId(
            originalWorkbookMissionId,
            challengerWorkbookId
        )) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_SUBMISSION_ALREADY_EXISTS);
        }
    }
}
