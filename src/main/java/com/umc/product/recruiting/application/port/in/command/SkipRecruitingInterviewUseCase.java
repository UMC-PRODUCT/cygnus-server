package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;

public interface SkipRecruitingInterviewUseCase {

    void skip(SkipRecruitingInterviewCommand command);
}
