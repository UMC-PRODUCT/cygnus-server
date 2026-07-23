package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;

public interface SubmitRecruitingApplicationEvaluationUseCase {

    void submit(SubmitRecruitingApplicationEvaluationCommand command);
}
