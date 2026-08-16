package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.RestoreRecruitingRoundCommand;

public interface RestoreRecruitingRoundUseCase {

    void restoreRound(RestoreRecruitingRoundCommand command);
}
