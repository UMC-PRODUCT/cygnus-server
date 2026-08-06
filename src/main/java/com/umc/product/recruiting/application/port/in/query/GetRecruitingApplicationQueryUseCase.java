package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;

public interface GetRecruitingApplicationQueryUseCase {

    RecruitingApplicationInfo getById(Long applicationId, Long requesterMemberId);

    RecruitingStatusSummaryInfo getStatusSummary(RecruitingStatusSummaryQuery query);

    boolean isRoundBelongsToSeason(Long roundId, Long seasonId);

    boolean isApplicationBelongsToSeason(Long applicationId, Long seasonId);
}
