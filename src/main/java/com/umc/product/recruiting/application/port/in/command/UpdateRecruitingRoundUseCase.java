package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundCommand;

public interface UpdateRecruitingRoundUseCase {

    void updateRound(UpdateRecruitingRoundCommand command);
}
