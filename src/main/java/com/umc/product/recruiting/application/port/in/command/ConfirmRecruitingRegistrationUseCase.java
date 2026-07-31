package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;

public interface ConfirmRecruitingRegistrationUseCase {

    void confirmRegistration(ConfirmRecruitingRegistrationCommand command);
}
