package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record CreateRecruitingRoundGraphQlRequest(
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
    Long availabilityFormId,
    String announcement,
    String contactText
) {

    public CreateRecruitingRoundCommand toCommand(Long seasonId) {
        return CreateRecruitingRoundCommand.builder()
            .seasonId(seasonId)
            .type(type)
            .roundNo(roundNo)
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
                availabilityFormId,
                announcement,
                contactText
            ))
            .build();
    }
}
