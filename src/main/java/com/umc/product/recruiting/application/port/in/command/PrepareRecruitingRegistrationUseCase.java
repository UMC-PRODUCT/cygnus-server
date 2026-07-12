package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;

public interface PrepareRecruitingRegistrationUseCase {

    void prepareRegistration(PrepareRecruitingRegistrationCommand command);
}
