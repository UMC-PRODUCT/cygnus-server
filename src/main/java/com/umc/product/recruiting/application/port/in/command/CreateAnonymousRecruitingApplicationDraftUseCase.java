package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CreateAnonymousRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;

public interface CreateAnonymousRecruitingApplicationDraftUseCase {

    RecruitingApplicationCreatedInfo createAnonymousDraft(
        CreateAnonymousRecruitingApplicationDraftCommand command
    );
}
