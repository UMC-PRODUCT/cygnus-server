package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;

public interface ConfirmRecruitingInterviewSchedulesUseCase {

    void confirmAll(ConfirmRecruitingInterviewSchedulesCommand command);
}
