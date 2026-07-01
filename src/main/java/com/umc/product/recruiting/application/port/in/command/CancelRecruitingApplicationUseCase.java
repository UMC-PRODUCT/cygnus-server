package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

public interface CancelRecruitingApplicationUseCase {

    RecruitingApplicationInfo cancel(CancelRecruitingApplicationCommand command);
}
