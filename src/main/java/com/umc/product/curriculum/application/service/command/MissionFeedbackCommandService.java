package com.umc.product.curriculum.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.curriculum.application.port.in.command.ManageMissionFeedbackUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.CreateMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.DeleteMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.SaveMissionFeedbackPort;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionFeedbackCommandService implements ManageMissionFeedbackUseCase {

    private final LoadMissionSubmissionPort loadMissionSubmissionPort;
    private final LoadMissionFeedbackPort loadMissionFeedbackPort;
    private final SaveMissionFeedbackPort saveMissionFeedbackPort;
    private final MissionMutationPolicy missionMutationPolicy;

    @Override
    public Long create(CreateMissionFeedbackCommand command) {
        MissionSubmission submission = loadMissionSubmissionPort.getByIdForUpdate(command.missionSubmissionId());
        MissionFeedback feedback = MissionFeedback.create(
            submission, command.reviewerMemberId(), command.content(), command.result()
        );
        return saveMissionFeedbackPort.save(feedback).getId();
    }

    @Override
    public void edit(EditMissionFeedbackCommand command) {
        MissionFeedback feedback = loadMissionFeedbackPort.getById(command.missionFeedbackId());
        validateReviewer(feedback, command.reviewerMemberId());
        missionMutationPolicy.validateFeedbackEdit(feedback);
        feedback.edit(command.content());
        saveMissionFeedbackPort.save(feedback);
    }

    @Override
    public void delete(DeleteMissionFeedbackCommand command) {
        MissionFeedback feedback = loadMissionFeedbackPort.getById(command.missionFeedbackId());
        validateReviewer(feedback, command.operatorMemberId());
        missionMutationPolicy.validateFeedbackDelete(feedback);
        saveMissionFeedbackPort.delete(feedback);
    }

    private void validateReviewer(MissionFeedback feedback, Long requesterMemberId) {
        if (!feedback.isReviewedBy(requesterMemberId)) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
    }
}
