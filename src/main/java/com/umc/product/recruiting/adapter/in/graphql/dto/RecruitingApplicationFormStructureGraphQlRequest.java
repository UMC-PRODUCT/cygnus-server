package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;

public record RecruitingApplicationFormStructureGraphQlRequest(
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice
) {
}
