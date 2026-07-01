package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewEvaluationCommand;

public interface SubmitRecruitingInterviewEvaluationUseCase {

    void submitEvaluation(SubmitRecruitingInterviewEvaluationCommand command);
}
