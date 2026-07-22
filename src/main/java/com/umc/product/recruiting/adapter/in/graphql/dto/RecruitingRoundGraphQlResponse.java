package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundGroupInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundResourceInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingRoundGraphQlResponse(
    Long id,
    Long seasonId,
    Long gisuId,
    Long schoolId,
    String title,
    RecruitingRoundType type,
    Integer roundNo,
    RecruitingRoundStatus status,
    List<ChallengerTrack> recruitableTracks,
    boolean secondChoiceEnabled,
    Instant documentStartAt,
    Instant documentEndAt,
    Instant documentResultPublishedAt,
    boolean interviewRequired,
    Instant interviewStartAt,
    Instant interviewEndAt,
    Instant finalResultPublishedAt,
    Long availabilityFormId,
    String announcement,
    String contactText,
    boolean applicationOpen,
    RecruitingApplicationFormGraphQlResponse applicationForm
) {

    public static RecruitingRoundGraphQlResponse from(RecruitingRoundResourceInfo info) {
        RecruitingRoundGraphQlResponse response = from(
            info.round(),
            info.seasonId(),
            info.gisuId(),
            info.schoolId()
        );
        return response.withApplicationForm(info.applicationForm(), info.applicationOpen());
    }

    public static RecruitingRoundGraphQlResponse from(
        RecruitingRoundConfigurationInfo info,
        Long seasonId,
        Long gisuId,
        Long schoolId
    ) {
        return new RecruitingRoundGraphQlResponse(
            info.id(),
            seasonId,
            gisuId,
            schoolId,
            info.title(),
            info.type(),
            info.roundNo(),
            info.status(),
            info.recruitableTracks(),
            info.secondChoiceEnabled(),
            info.documentStartAt(),
            info.documentEndAt(),
            info.documentResultPublishedAt(),
            info.interviewRequired(),
            info.interviewStartAt(),
            info.interviewEndAt(),
            info.finalResultPublishedAt(),
            info.availabilityFormId(),
            info.announcement(),
            info.contactText(),
            false,
            null
        );
    }

    public static RecruitingRoundGraphQlResponse from(
        RecruitingPublicRoundInfo info,
        RecruitingPublicRoundGroupInfo group
    ) {
        return new RecruitingRoundGraphQlResponse(
            info.roundId(),
            group.seasonId(),
            group.gisuId(),
            group.schoolId(),
            info.title(),
            info.type(),
            info.roundNo(),
            info.status(),
            info.recruitableTracks(),
            info.secondChoiceEnabled(),
            info.documentStartAt(),
            info.documentEndAt(),
            info.documentResultPublishedAt(),
            info.interviewRequired(),
            info.interviewStartAt(),
            info.interviewEndAt(),
            info.finalResultPublishedAt(),
            null,
            info.announcement(),
            null,
            info.applicationOpen(),
            new RecruitingApplicationFormGraphQlResponse(
                info.applicationFormId(),
                info.roundId(),
                info.formId(),
                com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus.PUBLISHED
            )
        );
    }

    private RecruitingRoundGraphQlResponse withApplicationForm(
        RecruitingApplicationFormInfo info,
        boolean open
    ) {
        return new RecruitingRoundGraphQlResponse(
            id,
            seasonId,
            gisuId,
            schoolId,
            title,
            type,
            roundNo,
            status,
            recruitableTracks,
            secondChoiceEnabled,
            documentStartAt,
            documentEndAt,
            documentResultPublishedAt,
            interviewRequired,
            interviewStartAt,
            interviewEndAt,
            finalResultPublishedAt,
            availabilityFormId,
            announcement,
            contactText,
            open,
            info == null ? null : RecruitingApplicationFormGraphQlResponse.from(info)
        );
    }
}
