package com.umc.product.recruiting.application.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonTrackQuotaInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.domain.RecruitingSeason;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingSeasonQueryService implements GetRecruitingSeasonConfigurationUseCase {

    private final LoadRecruitingSeasonPort loadSeasonPort;
    private final LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    private final LoadRecruitingRoundPort loadRoundPort;

    @Override
    public RecruitingSeasonConfigurationInfo getBySeasonId(Long seasonId) {
        RecruitingSeason season = loadSeasonPort.getById(seasonId);
        return RecruitingSeasonConfigurationInfo.of(
            season,
            loadQuotaPort.listBySeasonId(seasonId).stream()
                .map(RecruitingSeasonTrackQuotaInfo::from)
                .toList(),
            loadRoundPort.listBySeasonId(seasonId).stream()
                .map(RecruitingRoundConfigurationInfo::from)
                .toList()
        );
    }
}
