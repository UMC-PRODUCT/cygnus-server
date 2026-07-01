package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingApplicationFormInfo(
    Long applicationFormId,
    Long roundId,
    RecruitingRoundType roundType,
    Integer roundNo,
    Long formId,
    ChallengerTrack track,
    RecruitingApplicationFormStatus status
) {

    public static RecruitingApplicationFormInfo from(RecruitingApplicationForm applicationForm) {
        return new RecruitingApplicationFormInfo(
            applicationForm.getId(),
            applicationForm.getRound().getId(),
            applicationForm.getRound().getType(),
            applicationForm.getRound().getRoundNo(),
            applicationForm.getFormId(),
            applicationForm.getTrack(),
            applicationForm.getStatus()
        );
    }
}
