package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.CastDemodayVoteCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteInfo;

public interface CastDemodayVoteUseCase {

    DemodayVoteInfo cast(CastDemodayVoteCommand command);
}
