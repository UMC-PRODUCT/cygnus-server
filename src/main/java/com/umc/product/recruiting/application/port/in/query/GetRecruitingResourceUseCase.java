package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResourceInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundResourceInfo;

public interface GetRecruitingResourceUseCase {

    RecruitingRoundResourceInfo getRound(Long roundId, Long requesterMemberId);

    RecruitingApplicationResourceInfo getApplication(Long applicationId, Long requesterMemberId);

    RecruitingPublicApplicationInfo getApplicantView(Long applicationId, Long requesterMemberId);
}
