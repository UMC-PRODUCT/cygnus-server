package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;

public interface SearchRecruitingSeasonUseCase {

    List<RecruitingSeasonSummaryInfo> searchSeasons(RecruitingSeasonSearchQuery query);
}
