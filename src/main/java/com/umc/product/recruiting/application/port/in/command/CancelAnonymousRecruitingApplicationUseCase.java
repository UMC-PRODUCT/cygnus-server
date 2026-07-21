package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CancelAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

public interface CancelAnonymousRecruitingApplicationUseCase {

    RecruitingApplicationInfo cancelAnonymous(CancelAnonymousRecruitingApplicationCommand command);
}
