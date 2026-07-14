package com.umc.product.project.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.project.domain.ProjectMatchingRound;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;

import lombok.Builder;

@Builder
public record ProjectMatchingRoundInfo(
    Long id,
    String name,
    String description,
    MatchingType type,
    MatchingPhase phase,
    Long gisuId,
    Long chapterId,
    Instant startsAt,
    Instant endsAt,
    Instant decisionDeadline,
    Instant autoDecisionExecutedAt,
    Long autoDecisionExecutedMemberId,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProjectMatchingRoundInfo from(ProjectMatchingRound matchingRound) {
        return ProjectMatchingRoundInfo.builder()
            .id(matchingRound.getId())
            .name(matchingRound.getName())
            .description(matchingRound.getDescription())
            .type(matchingRound.getType())
            .phase(matchingRound.getPhase())
            .gisuId(matchingRound.getGisuId())
            .chapterId(matchingRound.getChapterId())
            .startsAt(matchingRound.getStartsAt())
            .endsAt(matchingRound.getEndsAt())
            .decisionDeadline(matchingRound.getDecisionDeadline())
            .autoDecisionExecutedAt(matchingRound.getAutoDecisionExecutedAt())
            .autoDecisionExecutedMemberId(matchingRound.getAutoDecisionExecutedMemberId())
            .createdAt(matchingRound.getCreatedAt())
            .updatedAt(matchingRound.getUpdatedAt())
            .build();
    }
}
