package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;

public interface UpsertRecruitingApplicationFormUseCase {

    Long upsert(UpsertRecruitingApplicationFormCommand command);
}
