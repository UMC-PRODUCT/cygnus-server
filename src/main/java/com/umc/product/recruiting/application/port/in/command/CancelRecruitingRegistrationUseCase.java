package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingRegistrationCommand;

public interface CancelRecruitingRegistrationUseCase {

    void cancelRegistration(CancelRecruitingRegistrationCommand command);
}
