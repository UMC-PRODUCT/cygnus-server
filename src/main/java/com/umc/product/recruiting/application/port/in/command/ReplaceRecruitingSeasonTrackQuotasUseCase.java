package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;

public interface ReplaceRecruitingSeasonTrackQuotasUseCase {

    void replaceQuotas(ReplaceRecruitingSeasonTrackQuotasCommand command);
}
