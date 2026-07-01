package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingApplicationFormResponse(
    Long applicationFormId,
    Long roundId,
    RecruitingRoundType roundType,
    Integer roundNo,
    Long formId,
    ChallengerTrack track,
    RecruitingApplicationFormStatus status
) {

    public static RecruitingApplicationFormResponse from(RecruitingApplicationFormInfo info) {
        return new RecruitingApplicationFormResponse(
            info.applicationFormId(),
            info.roundId(),
            info.roundType(),
            info.roundNo(),
            info.formId(),
            info.track(),
            info.status()
        );
    }
}
