package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResourceInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationGraphQlResponse(
    Long id,
    Long roundId,
    Long seasonId,
    RecruitingApplicationStatus status,
    RecruitingApplicationRegistrationStatus registrationStatus,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    ChallengerTrack acceptedTrack,
    boolean applicantAccess,
    boolean reviewAccess,
    RecruitingApplicationPrivateGraphQlResponse credentialPrivate,
    RecruitingApplicationReviewGraphQlResponse preloadedReview
) {

    public static RecruitingApplicationGraphQlResponse from(RecruitingApplicationInfo info) {
        return new RecruitingApplicationGraphQlResponse(
            info.applicationId(),
            null,
            null,
            info.status(),
            info.registrationStatus(),
            info.firstChoice(),
            info.secondChoice(),
            info.acceptedTrack(),
            true,
            false,
            null,
            null
        );
    }

    public static RecruitingApplicationGraphQlResponse from(RecruitingApplicationResourceInfo info) {
        return new RecruitingApplicationGraphQlResponse(
            info.applicationId(),
            info.roundId(),
            info.seasonId(),
            info.status(),
            info.registrationStatus(),
            info.firstChoice(),
            info.secondChoice(),
            info.acceptedTrack(),
            info.applicantAccess(),
            info.reviewAccess(),
            null,
            null
        );
    }

    public static RecruitingApplicationGraphQlResponse from(RecruitingPublicApplicationInfo info) {
        return new RecruitingApplicationGraphQlResponse(
            info.applicationId(),
            info.roundId(),
            info.seasonId(),
            info.status(),
            info.registrationStatus(),
            info.firstChoice(),
            info.secondChoice(),
            info.acceptedTrack(),
            true,
            false,
            RecruitingApplicationPrivateGraphQlResponse.from(info),
            null
        );
    }

    public static RecruitingApplicationGraphQlResponse from(RecruitingApplicationSummaryInfo info, Long roundId) {
        return new RecruitingApplicationGraphQlResponse(
            info.applicationId(),
            roundId,
            null,
            info.status(),
            info.registrationStatus(),
            info.firstChoice(),
            info.secondChoice(),
            info.acceptedTrack(),
            false,
            true,
            null,
            RecruitingApplicationReviewGraphQlResponse.from(info)
        );
    }

}
