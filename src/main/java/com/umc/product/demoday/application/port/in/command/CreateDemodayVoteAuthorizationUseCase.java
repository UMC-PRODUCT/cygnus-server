package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayVoteAuthorizationCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteAuthorizationInfo;

public interface CreateDemodayVoteAuthorizationUseCase {

    DemodayVoteAuthorizationInfo create(CreateDemodayVoteAuthorizationCommand command);
}
