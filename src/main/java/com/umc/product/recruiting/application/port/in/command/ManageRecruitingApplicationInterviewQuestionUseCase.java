package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingInterviewQuestionsCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationInterviewQuestionCommand;

public interface ManageRecruitingApplicationInterviewQuestionUseCase {

    Long createApplicationQuestion(CreateRecruitingApplicationInterviewQuestionCommand command);

    void updateApplicationQuestion(UpdateRecruitingApplicationInterviewQuestionCommand command);

    void deactivateApplicationQuestion(DeactivateRecruitingApplicationInterviewQuestionCommand command);

    void replaceApplicationQuestions(ReplaceRecruitingInterviewQuestionsCommand.Application command);
}
