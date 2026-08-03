package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingInterviewSessionCommand;

public interface ManageRecruitingInterviewSessionUseCase {

    Long createSession(CreateRecruitingInterviewSessionCommand command);

    void updateSession(UpdateRecruitingInterviewSessionCommand command);

    void deleteSession(DeleteRecruitingInterviewSessionCommand command);
}
