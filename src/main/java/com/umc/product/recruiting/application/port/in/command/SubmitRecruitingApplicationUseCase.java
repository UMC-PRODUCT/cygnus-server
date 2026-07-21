package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

public interface SubmitRecruitingApplicationUseCase {

    RecruitingApplicationInfo submit(SubmitRecruitingApplicationCommand command);
}
