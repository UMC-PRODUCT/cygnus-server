package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;

public interface CreateRecruitingSeasonUseCase {

    Long createSeason(CreateRecruitingSeasonCommand command);
}
