package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

public interface UpdateAnonymousRecruitingApplicationUseCase {

    RecruitingApplicationInfo updateAnonymous(UpdateAnonymousRecruitingApplicationCommand command);
}
