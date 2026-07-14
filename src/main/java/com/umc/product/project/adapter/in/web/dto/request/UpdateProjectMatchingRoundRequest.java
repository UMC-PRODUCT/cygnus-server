package com.umc.product.project.adapter.in.web.dto.request;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectMatchingRoundCommand;
import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;

import jakarta.validation.constraints.Size;

public record UpdateProjectMatchingRoundRequest(
    @Size(max = 255, message = "매칭 차수 이름은 255자 이하여야 합니다") String name,

    @Size(max = 255, message = "매칭 차수 설명은 255자 이하여야 합니다") String description,

    MatchingType type,

    MatchingPhase phase,

    Instant startsAt,

    Instant endsAt,

    Instant decisionDeadline
) {
    public UpdateProjectMatchingRoundRequest {
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("매칭 차수 이름은 비어 있을 수 없습니다.");
        }
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String propertyName, Object value) {
        if ("gisuId".equals(propertyName) || "chapterId".equals(propertyName)) {
            throw new IllegalArgumentException("매칭 차수 수정 요청에는 gisuId 또는 chapterId를 포함할 수 없습니다.");
        }
    }

    public UpdateProjectMatchingRoundCommand toCommand(Long matchingRoundId, Long requesterMemberId) {
        return UpdateProjectMatchingRoundCommand.builder()
            .matchingRoundId(matchingRoundId)
            .requesterMemberId(requesterMemberId)
            .name(name)
            .description(description)
            .type(type)
            .phase(phase)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .decisionDeadline(decisionDeadline)
            .build();
    }
}
