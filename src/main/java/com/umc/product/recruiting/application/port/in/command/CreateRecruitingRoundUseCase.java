package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;

public interface CreateRecruitingRoundUseCase {

    Long createRound(CreateRecruitingRoundCommand command);
}
