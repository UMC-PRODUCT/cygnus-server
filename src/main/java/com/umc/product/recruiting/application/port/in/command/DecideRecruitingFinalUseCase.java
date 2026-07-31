package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;

public interface DecideRecruitingFinalUseCase {

    void decideFinal(DecideRecruitingFinalCommand command);
}
