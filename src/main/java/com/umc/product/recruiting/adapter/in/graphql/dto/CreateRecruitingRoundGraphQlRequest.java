package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record CreateRecruitingRoundGraphQlRequest(
    String seasonId,
    String title,
    RecruitingRoundType type,
    Integer roundNo,
    List<ChallengerTrack> recruitableTracks,
    boolean secondChoiceEnabled,
    Instant documentStartAt,
    Instant documentEndAt,
    Instant documentResultPublishedAt,
    boolean interviewRequired,
    Instant interviewStartAt,
    Instant interviewEndAt,
    Instant finalResultPublishedAt,
    String availabilityFormId,
    String availabilityScheduleQuestionId,
    String announcement,
    String contactText
) {

    public Long decodedSeasonId() {
        return GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
    }

    public CreateRecruitingRoundCommand toCommand() {
        return CreateRecruitingRoundCommand.builder()
            .seasonId(decodedSeasonId())
            .type(type)
            .roundNo(roundNo)
            .title(title)
            .configuration(RecruitingRoundConfigurationCommand.of(
                recruitableTracks,
                secondChoiceEnabled,
                documentStartAt,
                documentEndAt,
                documentResultPublishedAt,
                interviewRequired,
                interviewStartAt,
                interviewEndAt,
                finalResultPublishedAt,
                availabilityFormId == null
                    ? null
                    : GlobalId.decodeLong(availabilityFormId, GlobalIdTypes.FORM),
                availabilityScheduleQuestionId == null
                    ? null
                    : GlobalId.decodeLong(availabilityScheduleQuestionId, GlobalIdTypes.FORM_QUESTION),
                announcement,
                contactText
            ))
            .build();
    }
}
