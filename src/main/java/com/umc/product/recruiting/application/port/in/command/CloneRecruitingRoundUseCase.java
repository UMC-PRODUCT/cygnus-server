package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CloneRecruitingRoundCommand;

public interface CloneRecruitingRoundUseCase {

    Long cloneRound(CloneRecruitingRoundCommand command);
}
