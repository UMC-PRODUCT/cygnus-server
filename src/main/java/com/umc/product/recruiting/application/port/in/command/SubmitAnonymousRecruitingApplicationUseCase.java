package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

public interface SubmitAnonymousRecruitingApplicationUseCase {

    RecruitingApplicationInfo submitAnonymous(SubmitAnonymousRecruitingApplicationCommand command);
}
