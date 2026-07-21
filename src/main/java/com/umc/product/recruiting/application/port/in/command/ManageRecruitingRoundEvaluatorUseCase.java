package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;

public interface ManageRecruitingRoundEvaluatorUseCase {

    Long addEvaluator(RecruitingRoundEvaluatorCommand command);

    void removeEvaluator(RecruitingRoundEvaluatorCommand command);
}
