package com.umc.product.challenger.application.service;

import com.umc.product.challenger.application.port.in.query.GetUnusedChallengerRecordStatisticsUseCase;
import com.umc.product.challenger.application.port.out.LoadChallengerRecordPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengerRecordStatisticsQueryService implements GetUnusedChallengerRecordStatisticsUseCase {

    private final LoadChallengerRecordPort loadChallengerRecordPort;

    @Override
    public long getUnusedRecordCount() {
        return loadChallengerRecordPort.countUnused();
    }
}
