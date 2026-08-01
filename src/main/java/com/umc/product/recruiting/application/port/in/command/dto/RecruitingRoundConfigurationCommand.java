package com.umc.product.recruiting.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;

public record RecruitingRoundConfigurationCommand(
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
    Long availabilityScheduleQuestionId,
    String announcement,
    String contactText
) {

    public static RecruitingRoundConfigurationCommand of(
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
        Long availabilityScheduleQuestionId,
        String announcement,
        String contactText
    ) {
        return new RecruitingRoundConfigurationCommand(
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
            availabilityScheduleQuestionId,
            announcement,
            contactText
        );
    }

    public static RecruitingRoundConfigurationCommand of(
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
        return of(
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
            null,
            announcement,
            contactText
        );
    }

    public RecruitingRoundConfiguration toDomain() {
        return RecruitingRoundConfiguration.of(
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
            availabilityScheduleQuestionId,
            announcement,
            contactText
        );
    }
}
