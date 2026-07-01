package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonStatusCommand;

public interface UpdateRecruitingSeasonStatusUseCase {

    void updateSeasonStatus(UpdateRecruitingSeasonStatusCommand command);
}
