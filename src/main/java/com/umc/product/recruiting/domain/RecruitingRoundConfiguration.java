package com.umc.product.recruiting.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

public record RecruitingRoundConfiguration(
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

    public RecruitingRoundConfiguration {
        validateTracks(recruitableTracks);
        validateDocumentSchedule(
            documentStartAt,
            documentEndAt,
            documentResultPublishedAt,
            finalResultPublishedAt
        );
        validateInterviewShape(
            interviewRequired,
            interviewStartAt,
            interviewEndAt,
            availabilityFormId,
            availabilityScheduleQuestionId
        );
        boolean interviewWindowComplete = interviewStartAt != null && interviewEndAt != null;
        if (interviewRequired && interviewWindowComplete) {
            validateInterviewSchedule(
                documentResultPublishedAt,
                interviewStartAt,
                interviewEndAt,
                finalResultPublishedAt
            );
        }
        if (!interviewRequired && finalResultPublishedAt.isBefore(documentResultPublishedAt)) {
            throw invalidSchedule();
        }
        recruitableTracks = List.copyOf(recruitableTracks);
    }

    public static RecruitingRoundConfiguration of(
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
        return new RecruitingRoundConfiguration(
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

    public static RecruitingRoundConfiguration of(
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

    private static void validateTracks(List<ChallengerTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            throw invalidTracks();
        }

        boolean containsNull = tracks.stream().anyMatch(Objects::isNull);
        if (containsNull) {
            throw invalidTracks();
        }

        boolean containsUnsupportedTrack = tracks.contains(ChallengerTrack.INFRA_PLUS);
        if (containsUnsupportedTrack) {
            throw invalidTracks();
        }

        if (new HashSet<>(tracks).size() != tracks.size()) {
            throw invalidTracks();
        }
    }

    private static RecruitingDomainException invalidTracks() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_TRACKS);
    }

    private static void validateDocumentSchedule(
        Instant documentStartAt,
        Instant documentEndAt,
        Instant documentResultPublishedAt,
        Instant finalResultPublishedAt
    ) {
        boolean requiredTimeMissing = documentStartAt == null
            || documentEndAt == null
            || documentResultPublishedAt == null
            || finalResultPublishedAt == null;
        if (requiredTimeMissing) {
            throw invalidSchedule();
        }

        if (!documentStartAt.isBefore(documentEndAt)) {
            throw invalidSchedule();
        }

        if (documentResultPublishedAt.isBefore(documentEndAt)) {
            throw invalidSchedule();
        }
    }

    private static void validateInterviewShape(
        boolean interviewRequired,
        Instant interviewStartAt,
        Instant interviewEndAt,
        Long availabilityFormId,
        Long availabilityScheduleQuestionId
    ) {
        boolean interviewTimeMissing = interviewStartAt == null || interviewEndAt == null;
        if (interviewRequired && interviewTimeMissing) {
            throw invalidSchedule();
        }

        boolean interviewTimePresent = interviewStartAt != null || interviewEndAt != null;
        if (!interviewRequired && interviewTimePresent) {
            throw invalidSchedule();
        }

        boolean availabilityMappingMissingPart = (availabilityFormId == null)
            != (availabilityScheduleQuestionId == null);
        if (availabilityMappingMissingPart) {
            throw invalidSchedule();
        }

        boolean availabilityMappingNotPositive = availabilityFormId != null
            && (availabilityFormId <= 0 || availabilityScheduleQuestionId <= 0);
        if (availabilityMappingNotPositive) {
            throw invalidSchedule();
        }

        if (!interviewRequired && availabilityFormId != null) {
            throw invalidSchedule();
        }
    }

    private static void validateInterviewSchedule(
        Instant documentResultPublishedAt,
        Instant interviewStartAt,
        Instant interviewEndAt,
        Instant finalResultPublishedAt
    ) {
        if (interviewStartAt.isBefore(documentResultPublishedAt)) {
            throw invalidSchedule();
        }

        if (!interviewStartAt.isBefore(interviewEndAt)) {
            throw invalidSchedule();
        }

        if (finalResultPublishedAt.isBefore(interviewEndAt)) {
            throw invalidSchedule();
        }
    }

    private static RecruitingDomainException invalidSchedule() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
    }
}
