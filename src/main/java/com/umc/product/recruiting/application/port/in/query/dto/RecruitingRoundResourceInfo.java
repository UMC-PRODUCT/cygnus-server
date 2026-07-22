package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;

public record RecruitingRoundResourceInfo(
    Long seasonId,
    Long gisuId,
    Long schoolId,
    RecruitingRoundConfigurationInfo round,
    RecruitingApplicationFormInfo applicationForm,
    boolean applicationOpen
) {

    public static RecruitingRoundResourceInfo of(
        RecruitingRound round,
        RecruitingApplicationForm applicationForm,
        Instant now
    ) {
        return new RecruitingRoundResourceInfo(
            round.getSeason().getId(),
            round.getSeason().getGisuId(),
            round.getSeason().getSchoolId(),
            RecruitingRoundConfigurationInfo.from(round),
            applicationForm == null ? null : RecruitingApplicationFormInfo.from(applicationForm),
            applicationForm != null && round.isLocalApplicationPeriodOpenAt(now, applicationForm.getStatus())
        );
    }
}
