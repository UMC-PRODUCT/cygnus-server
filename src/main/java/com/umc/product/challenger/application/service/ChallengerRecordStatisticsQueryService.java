package com.umc.product.challenger.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.query.GetUnusedChallengerRecordStatisticsUseCase;
import com.umc.product.challenger.application.port.in.query.dto.UnusedChallengerRecordCountInfo;
import com.umc.product.challenger.application.port.out.LoadChallengerRecordPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengerRecordStatisticsQueryService implements GetUnusedChallengerRecordStatisticsUseCase {

    private final LoadChallengerRecordPort loadChallengerRecordPort;

    @Override
    public List<UnusedChallengerRecordCountInfo> getUnusedCountByGisuAndSchool() {
        return loadChallengerRecordPort.aggregateUnusedCountByGisuAndSchool().stream()
            .map(row -> new UnusedChallengerRecordCountInfo(row.gisuId(), row.schoolId(), row.unusedCount()))
            .toList();
    }
}
