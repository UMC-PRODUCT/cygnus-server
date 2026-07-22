package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SetRecruitingRoundEvaluatorsCommand;

public interface ManageRecruitingRoundEvaluatorUseCase {

    Long addEvaluator(RecruitingRoundEvaluatorCommand command);

    void removeEvaluator(RecruitingRoundEvaluatorCommand command);

    void setEvaluators(SetRecruitingRoundEvaluatorsCommand command);
}
