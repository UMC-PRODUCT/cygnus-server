package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;

public interface GetRecruitingSeasonConfigurationUseCase {

    RecruitingSeasonConfigurationInfo getBySeasonId(Long seasonId);
}
