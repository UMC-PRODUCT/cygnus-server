package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;

public interface GetRecruitingApplicationQuestionScopeUseCase {

    RecruitingApplicationQuestionScopeInfo getQuestionScope(
        Long applicationFormId,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    );
}
