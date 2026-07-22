package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingSeasonCommand;

public interface UpdateRecruitingSeasonUseCase {

    void updateSeason(UpdateRecruitingSeasonCommand command);

    void updateSeasonAndQuotas(
        UpdateRecruitingSeasonCommand seasonCommand,
        ReplaceRecruitingSeasonTrackQuotasCommand quotaCommand
    );
}
