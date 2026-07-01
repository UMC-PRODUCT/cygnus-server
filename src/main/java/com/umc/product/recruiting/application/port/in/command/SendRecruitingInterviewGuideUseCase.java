package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.SendRecruitingInterviewGuideCommand;

public interface SendRecruitingInterviewGuideUseCase {

    void sendGuide(SendRecruitingInterviewGuideCommand command);
}
