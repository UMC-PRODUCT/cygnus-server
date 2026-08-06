package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingRoundCommand;

public interface DeleteRecruitingRoundUseCase {

    void deleteRound(DeleteRecruitingRoundCommand command);
}
