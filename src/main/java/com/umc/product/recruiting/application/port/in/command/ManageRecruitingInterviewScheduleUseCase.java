package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;

public interface ManageRecruitingInterviewScheduleUseCase {

    Long requestAvailability(RequestRecruitingInterviewScheduleCommand command);

    void submitAvailability(SubmitRecruitingInterviewAvailabilityCommand command);

    void confirm(ConfirmRecruitingInterviewScheduleCommand command);
}
