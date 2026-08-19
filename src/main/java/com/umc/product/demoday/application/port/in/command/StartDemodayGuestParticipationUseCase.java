package com.umc.product.demoday.application.port.in.command;

import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationCommand;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationInfo;

public interface StartDemodayGuestParticipationUseCase {

    StartDemodayGuestParticipationInfo start(StartDemodayGuestParticipationCommand command);
}
