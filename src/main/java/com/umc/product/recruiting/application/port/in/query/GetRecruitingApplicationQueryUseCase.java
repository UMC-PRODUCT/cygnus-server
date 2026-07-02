package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResultInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;

public interface GetRecruitingApplicationQueryUseCase {

    RecruitingApplicationResultInfo getAnonymousResult(String applicationNo, String applicantIdentityKey);

    RecruitingStatusSummaryInfo getStatusSummary(Long gisuId, Long schoolId);

    boolean isRoundBelongsToSeason(Long roundId, Long seasonId);

    boolean isApplicationBelongsToSeason(Long applicationId, Long seasonId);
}
