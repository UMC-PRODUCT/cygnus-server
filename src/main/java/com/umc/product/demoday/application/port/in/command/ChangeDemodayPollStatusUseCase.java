package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayPollStatusCommand;

public interface ChangeDemodayPollStatusUseCase {

    void changeStatus(ChangeDemodayPollStatusCommand command);

}
