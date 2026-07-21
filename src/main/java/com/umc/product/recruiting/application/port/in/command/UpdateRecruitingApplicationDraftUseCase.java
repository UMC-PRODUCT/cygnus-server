package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;

public interface UpdateRecruitingApplicationDraftUseCase {

    RecruitingApplicationInfo updateDraft(UpdateRecruitingApplicationDraftCommand command);
}
