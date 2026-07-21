package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;

public interface CloseRecruitingApplicationFormUseCase {

    void close(CloseRecruitingApplicationFormCommand command);
}
