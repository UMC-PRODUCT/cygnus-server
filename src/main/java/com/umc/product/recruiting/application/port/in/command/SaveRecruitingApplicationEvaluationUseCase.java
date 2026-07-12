package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingApplicationEvaluationCommand;

public interface SaveRecruitingApplicationEvaluationUseCase {

    Long saveDraft(SaveRecruitingApplicationEvaluationCommand command);
}
