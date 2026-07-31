package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundInterviewQuestionCommand;

public interface ManageRecruitingRoundInterviewQuestionUseCase {

    Long createRoundQuestion(CreateRecruitingRoundInterviewQuestionCommand command);

    void updateRoundQuestion(UpdateRecruitingRoundInterviewQuestionCommand command);

    void deactivateRoundQuestion(DeactivateRecruitingRoundInterviewQuestionCommand command);
}
