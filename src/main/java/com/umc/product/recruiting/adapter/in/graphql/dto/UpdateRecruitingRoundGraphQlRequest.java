package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundConfigurationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundCommand;

public record UpdateRecruitingRoundGraphQlRequest(
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

    public UpdateRecruitingRoundCommand toCommand(Long seasonId, Long roundId) {
        return UpdateRecruitingRoundCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
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
