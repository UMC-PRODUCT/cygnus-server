package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayVoteStatusCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteStatusInfo;

public interface ChangeDemodayVoteStatusUseCase {

    DemodayVoteStatusInfo revoke(ChangeDemodayVoteStatusCommand command);

    DemodayVoteStatusInfo restore(ChangeDemodayVoteStatusCommand command);
}
