package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayPollCommand;

public interface CreateDemodayPollUseCase {

    Long create(CreateDemodayPollCommand command);

}
