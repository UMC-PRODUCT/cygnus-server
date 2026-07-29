package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsQuery;

public interface GetRecruitingEvaluationStatisticsUseCase {

    RecruitingEvaluationStatisticsInfo getEvaluationStatistics(RecruitingEvaluationStatisticsQuery query);
}
