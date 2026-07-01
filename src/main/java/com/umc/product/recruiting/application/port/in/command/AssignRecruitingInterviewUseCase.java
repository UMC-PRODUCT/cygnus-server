package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.AssignRecruitingInterviewCommand;

public interface AssignRecruitingInterviewUseCase {

    Long assign(AssignRecruitingInterviewCommand command);
}
