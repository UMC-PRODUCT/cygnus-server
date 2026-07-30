package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.application.port.out.dto.RecruitingEvaluationStatisticsRow;

public interface LoadRecruitingEvaluationStatisticsPort {

    List<RecruitingEvaluationStatisticsRow> listByGisuId(Long gisuId);
}
