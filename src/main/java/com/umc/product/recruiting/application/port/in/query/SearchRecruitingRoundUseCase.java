package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSummaryInfo;

public interface SearchRecruitingRoundUseCase {

    List<RecruitingRoundSummaryInfo> searchRounds(RecruitingRoundSearchQuery query);
}
