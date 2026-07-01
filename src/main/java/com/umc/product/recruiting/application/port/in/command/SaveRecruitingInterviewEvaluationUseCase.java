package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingInterviewEvaluationCommand;

public interface SaveRecruitingInterviewEvaluationUseCase {

    void saveEvaluation(SaveRecruitingInterviewEvaluationCommand command);
}
