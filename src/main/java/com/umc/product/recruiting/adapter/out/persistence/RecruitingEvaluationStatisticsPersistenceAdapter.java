package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingEvaluationStatisticsPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingEvaluationStatisticsRow;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingEvaluationStatisticsPersistenceAdapter implements LoadRecruitingEvaluationStatisticsPort {

    private final RecruitingEvaluationStatisticsQueryRepository queryRepository;

    @Override
    public List<RecruitingEvaluationStatisticsRow> listByGisuId(Long gisuId) {
        return queryRepository.listByGisuId(gisuId);
    }
}
