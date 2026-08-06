package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.UnpublishRecruitingApplicationFormCommand;

public interface UnpublishRecruitingApplicationFormUseCase {

    void unpublish(UnpublishRecruitingApplicationFormCommand command);
}
