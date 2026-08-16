package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;

public interface UpdateRecruitingRoundStatusUseCase {

    void updateRoundStatus(UpdateRecruitingRoundStatusCommand command);
}
