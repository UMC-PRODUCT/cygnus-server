package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundGroupSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;

public interface SearchRecruitingRoundGroupUseCase {

    List<RecruitingSeasonSummaryInfo> searchRoundGroups(RecruitingRoundGroupSearchQuery query);
}
