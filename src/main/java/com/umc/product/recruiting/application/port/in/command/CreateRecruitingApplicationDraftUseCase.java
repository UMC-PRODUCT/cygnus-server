package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

public interface CreateRecruitingApplicationDraftUseCase {

    RecruitingApplicationInfo createDraft(CreateRecruitingApplicationDraftCommand command);
}
