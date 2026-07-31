package com.umc.product.challenger.application.port.in.command;

import com.umc.product.challenger.application.port.in.command.dto.AddChallengerTrackCommand;

public interface AddChallengerTrackUseCase {

    void addTrack(AddChallengerTrackCommand command);
}
