package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;

public interface GetRecruitingApplicationQueryUseCase {

    RecruitingApplicationInfo getById(Long applicationId, Long requesterMemberId);

    RecruitingStatusSummaryInfo getStatusSummary(Long gisuId, Long schoolId, Long requesterMemberId);

    RecruitingStatusSummaryInfo getStatusSummary(
        Long gisuId,
        Long schoolId,
        Long roundId,
        Long requesterMemberId
    );

    boolean isRoundBelongsToSeason(Long roundId, Long seasonId);

    boolean isApplicationBelongsToSeason(Long applicationId, Long seasonId);
}
